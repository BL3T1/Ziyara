/**
 * Ziyara API — k6 Stress Test  (v4)
 *
 * Profiles (K6_PROFILE env var):
 *   smoke   — 2 VU/scenario, ~1 min.  Validates every endpoint returns correct status.
 *   load    — ~360 VU peak.  Normal production traffic mix.
 *   stress  — ~1460 VU peak.  Well beyond capacity; finds breaking points.
 *   soak    — ~350 VU for 30 min.  Detects memory leaks and connection pool exhaustion.
 *   extreme — ~5000 VU peak.  Massive overload test – finds system collapse points.
 *
 * Scenarios (9):
 *   browsing            — anonymous web: service list, search, filter, infinite scroll
 *   mobile_app          — iOS/Android app session: home, search, detail, book, notify, track
 *   landing             — public landing: content pages, currency, detail, availability
 *   authenticated_reads — web logged-in: profile, bookings, notifications, nav, consents
 *   booking_writes      — checkout path: detail → availability → preview → POST /bookings
 *   admin_dashboard     — all dashboard aggregations: bootstrap, kpis, live, charts (cached)
 *   admin_operations    — queue work: complaints, tickets, reports, payouts, users, audit
 *   provider_portal     — provider: dashboard, services, bookings, earnings, staff, cash
 *   auth_flow           — login → permissions → refresh → logout (token lifecycle)
 *
 * Usage:
 *   k6 run k6/stress-test.js
 *   k6 run --env K6_PROFILE=smoke   k6/stress-test.js
 *   k6 run --env K6_PROFILE=load    k6/stress-test.js
 *   k6 run --env K6_PROFILE=stress  k6/stress-test.js
 *   k6 run --env K6_PROFILE=soak    k6/stress-test.js
 *   k6 run --env K6_PROFILE=extreme k6/stress-test.js
 *
 * Provider portal (requires a real provider account):
 *   k6 run --env PROVIDER_EMAIL=provider@example.com \
 *          --env PROVIDER_PASSWORD=Pass@1234 \
 *          k6/stress-test.js
 *
 * Without PROVIDER_EMAIL the portal scenario falls back to admin KPI endpoints.
 *
 * VU peaks per profile:
 *   smoke:   18 total (2 per scenario)
 *   load:   ~360 total
 *   stress: ~1460 total
 *   soak:   ~345 sustained
 *   extreme:~5000 total
 */

import http from 'k6/http';
import { check, sleep, group, fail } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// ── Environment ────────────────────────────────────────────────────────────────

const BASE_URL       = __ENV.BASE_URL          || 'http://localhost:7005/api/v1';
const ADMIN_EMAIL    = __ENV.ADMIN_EMAIL        || 'super_admin@ziyarah.com';
const ADMIN_PASS     = __ENV.ADMIN_PASSWORD     || 'Admin@1234';
const PROVIDER_EMAIL = __ENV.PROVIDER_EMAIL     || '';
const PROVIDER_PASS  = __ENV.PROVIDER_PASSWORD  || '';
const PROFILE        = __ENV.K6_PROFILE         || 'stress';

// Additional accounts used to build the shared token pool in setup().
// Spreading logins across multiple accounts keeps each account well under the
// 40-req/min per-IP login rate limit and produces more realistic JWT diversity.
//
// Set via env vars (comma-separated email:password pairs):
//   K6_EXTRA_ACCOUNTS="user1@test.com:Pass@1234,user2@test.com:Pass@1234"
// Falls back to a set of seeded test accounts when the env var is absent.
const _extraRaw = __ENV.K6_EXTRA_ACCOUNTS || '';
const EXTRA_ACCOUNTS = _extraRaw
  ? _extraRaw.split(',').map((s) => {
      const idx = s.lastIndexOf(':');
      return { email: s.slice(0, idx).trim(), pass: s.slice(idx + 1).trim() };
    }).filter((a) => a.email && a.pass)
  : [
      { email: 'staff1@ziyarah.com',    pass: 'Staff@1234' },
      { email: 'staff2@ziyarah.com',    pass: 'Staff@1234' },
      { email: 'manager1@ziyarah.com',  pass: 'Manager@1234' },
    ];

// ── Custom metrics ─────────────────────────────────────────────────────────────

const serverErrors    = new Counter('server_errors');
const rateLimited     = new Counter('rate_limited');
const bookingsCreated = new Counter('bookings_created');
const tokenRefreshed  = new Counter('token_refreshed');
const mobileRequests  = new Counter('mobile_requests');
const adminDashHits   = new Counter('admin_dash_hits');
const adminOpsHits    = new Counter('admin_ops_hits');
const portalHits      = new Counter('portal_hits');
const landingHits     = new Counter('landing_hits');

const bookingLatency   = new Trend('booking_create_ms',  true);
const authLatency      = new Trend('auth_login_ms',      true);
const dashboardLatency = new Trend('dashboard_load_ms',  true);
const mobileLatency    = new Trend('mobile_session_ms',  true);
const portalLatency    = new Trend('portal_req_ms',      true);
const landingLatency   = new Trend('landing_req_ms',     true);

// ── Stage profiles ─────────────────────────────────────────────────────────────
//
// Traffic mix rationale (based on typical hospitality booking platform):
//   35% anonymous browsing  (web bots, window shoppers)
//   27% mobile app sessions (biggest growth channel)
//   17% landing / detail    (SEO traffic landing on listing pages)
//    7% authenticated reads  (logged-in web users)
//    4% booking writes       (checkout funnel)
//    5% provider portal      (providers managing their listings)
//    2% admin dashboard      (ops team, cached endpoints)
//    1% admin operations     (queue processing, low but heavy queries)
//    1% auth lifecycle       (login/refresh/logout churn)

const PROFILES = {
  smoke: {
    browsing:   [{ duration: '1m', target: 2 }],
    mobile:     [{ duration: '1m', target: 2 }],
    landing:    [{ duration: '1m', target: 2 }],
    authed:     [{ duration: '1m', target: 2 }],
    writes:     [{ duration: '1m', target: 2 }],
    adminDash:  [{ duration: '1m', target: 2 }],
    adminOps:   [{ duration: '1m', target: 2 }],
    portal:     [{ duration: '1m', target: 2 }],
    authFlow:   [{ duration: '1m', target: 2 }],
  },

  load: {
    browsing:   [{ duration: '1m', target: 40  }, { duration: '5m', target: 120 }, { duration: '1m', target: 0 }],
    mobile:     [{ duration: '1m', target: 30  }, { duration: '5m', target: 100 }, { duration: '1m', target: 0 }],
    landing:    [{ duration: '1m', target: 20  }, { duration: '5m', target: 60  }, { duration: '1m', target: 0 }],
    authed:     [{ duration: '1m', target: 10  }, { duration: '5m', target: 25  }, { duration: '1m', target: 0 }],
    writes:     [{ duration: '1m', target: 5   }, { duration: '5m', target: 15  }, { duration: '1m', target: 0 }],
    adminDash:  [{ duration: '1m', target: 3   }, { duration: '5m', target: 8   }, { duration: '1m', target: 0 }],
    adminOps:   [{ duration: '1m', target: 2   }, { duration: '5m', target: 5   }, { duration: '1m', target: 0 }],
    portal:     [{ duration: '1m', target: 5   }, { duration: '5m', target: 20  }, { duration: '1m', target: 0 }],
    authFlow:   [{ duration: '7m', target: 5   }],
  },

  stress: {
    browsing:   [
      { duration: '1m',  target: 80  },
      { duration: '2m',  target: 200 },
      { duration: '3m',  target: 500 },
      { duration: '2m',  target: 150 },
      { duration: '1m',  target: 0   },
    ],
    mobile:     [
      { duration: '1m',  target: 60  },
      { duration: '2m',  target: 150 },
      { duration: '3m',  target: 400 },
      { duration: '2m',  target: 100 },
      { duration: '1m',  target: 0   },
    ],
    landing:    [
      { duration: '1m',  target: 40  },
      { duration: '2m',  target: 100 },
      { duration: '3m',  target: 250 },
      { duration: '2m',  target: 80  },
      { duration: '1m',  target: 0   },
    ],
    authed:     [
      { duration: '1m',  target: 20  },
      { duration: '2m',  target: 50  },
      { duration: '3m',  target: 100 },
      { duration: '2m',  target: 30  },
      { duration: '1m',  target: 0   },
    ],
    writes:     [
      { duration: '1m',  target: 10  },
      { duration: '2m',  target: 25  },
      { duration: '3m',  target: 60  },
      { duration: '2m',  target: 20  },
      { duration: '1m',  target: 0   },
    ],
    adminDash:  [
      { duration: '1m',  target: 5   },
      { duration: '2m',  target: 15  },
      { duration: '3m',  target: 30  },
      { duration: '2m',  target: 10  },
      { duration: '1m',  target: 0   },
    ],
    adminOps:   [
      { duration: '1m',  target: 3   },
      { duration: '2m',  target: 10  },
      { duration: '3m',  target: 20  },
      { duration: '2m',  target: 5   },
      { duration: '1m',  target: 0   },
    ],
    portal:     [
      { duration: '1m',  target: 10  },
      { duration: '2m',  target: 30  },
      { duration: '3m',  target: 80  },
      { duration: '2m',  target: 25  },
      { duration: '1m',  target: 0   },
    ],
    authFlow:   [{ duration: '9m', target: 15 }],
  },

  soak: {
    browsing:   [{ duration: '2m', target: 100 }, { duration: '26m', target: 100 }, { duration: '2m', target: 0 }],
    mobile:     [{ duration: '2m', target: 80  }, { duration: '26m', target: 80  }, { duration: '2m', target: 0 }],
    landing:    [{ duration: '2m', target: 60  }, { duration: '26m', target: 60  }, { duration: '2m', target: 0 }],
    authed:     [{ duration: '2m', target: 40  }, { duration: '26m', target: 40  }, { duration: '2m', target: 0 }],
    writes:     [{ duration: '2m', target: 20  }, { duration: '26m', target: 20  }, { duration: '2m', target: 0 }],
    adminDash:  [{ duration: '2m', target: 15  }, { duration: '26m', target: 15  }, { duration: '2m', target: 0 }],
    adminOps:   [{ duration: '2m', target: 10  }, { duration: '26m', target: 10  }, { duration: '2m', target: 0 }],
    portal:     [{ duration: '2m', target: 15  }, { duration: '26m', target: 15  }, { duration: '2m', target: 0 }],
    authFlow:   [{ duration: '30m', target: 5  }],
  },

  // ── EXTREME profile: ~5000 VU peak ──────────────────────────────────────────
  // Scaled by factor ~3.4 from 'stress' to reach ~5000 total VUs.
  // Stages follow the same ramp-up/ramp-down pattern.
  extreme: {
    browsing:   [
      { duration: '1m',  target: 280 },
      { duration: '2m',  target: 700 },
      { duration: '3m',  target: 1700 },
      { duration: '2m',  target: 500 },
      { duration: '1m',  target: 0   },
    ],
    mobile:     [
      { duration: '1m',  target: 210 },
      { duration: '2m',  target: 520 },
      { duration: '3m',  target: 1360 },
      { duration: '2m',  target: 400 },
      { duration: '1m',  target: 0   },
    ],
    landing:    [
      { duration: '1m',  target: 140 },
      { duration: '2m',  target: 350 },
      { duration: '3m',  target: 850 },
      { duration: '2m',  target: 280 },
      { duration: '1m',  target: 0   },
    ],
    authed:     [
      { duration: '1m',  target: 70  },
      { duration: '2m',  target: 170 },
      { duration: '3m',  target: 350 },
      { duration: '2m',  target: 100 },
      { duration: '1m',  target: 0   },
    ],
    writes:     [
      { duration: '1m',  target: 35  },
      { duration: '2m',  target: 85  },
      { duration: '3m',  target: 210 },
      { duration: '2m',  target: 70  },
      { duration: '1m',  target: 0   },
    ],
    adminDash:  [
      { duration: '1m',  target: 18  },
      { duration: '2m',  target: 50  },
      { duration: '3m',  target: 105 },
      { duration: '2m',  target: 35  },
      { duration: '1m',  target: 0   },
    ],
    adminOps:   [
      { duration: '1m',  target: 10  },
      { duration: '2m',  target: 35  },
      { duration: '3m',  target: 70  },
      { duration: '2m',  target: 20  },
      { duration: '1m',  target: 0   },
    ],
    portal:     [
      { duration: '1m',  target: 35  },
      { duration: '2m',  target: 100 },
      { duration: '3m',  target: 280 },
      { duration: '2m',  target: 90  },
      { duration: '1m',  target: 0   },
    ],
    authFlow:   [{ duration: '9m', target: 52 }],
  },
};

const profileStages = PROFILES[PROFILE] || PROFILES.stress;

// ── Test options ────────────────────────────────────────────────────────────────

export const options = {
  scenarios: {
    browsing: {
      executor: 'ramping-vus',
      exec: 'browsing',
      startVUs: 0,
      stages: profileStages.browsing,
      gracefulRampDown: '15s',
    },

    mobile_app: {
      executor: 'ramping-vus',
      exec: 'mobileApp',
      startVUs: 0,
      startTime: '5s',
      stages: profileStages.mobile,
      gracefulRampDown: '15s',
    },

    landing: {
      executor: 'ramping-vus',
      exec: 'landingPages',
      startVUs: 0,
      startTime: '8s',
      stages: profileStages.landing,
      gracefulRampDown: '15s',
    },

    authenticated_reads: {
      executor: 'ramping-vus',
      exec: 'authenticatedReads',
      startVUs: 0,
      startTime: '15s',
      stages: profileStages.authed,
      gracefulRampDown: '15s',
    },

    booking_writes: {
      executor: 'ramping-vus',
      exec: 'bookingWrites',
      startVUs: 0,
      startTime: '25s',
      stages: profileStages.writes,
      gracefulRampDown: '20s',
    },

    admin_dashboard: {
      executor: 'ramping-vus',
      exec: 'adminDashboard',
      startVUs: 0,
      startTime: '20s',
      stages: profileStages.adminDash,
      gracefulRampDown: '15s',
    },

    admin_operations: {
      executor: 'ramping-vus',
      exec: 'adminOperations',
      startVUs: 0,
      startTime: '30s',
      stages: profileStages.adminOps,
      gracefulRampDown: '15s',
    },

    provider_portal: {
      executor: 'ramping-vus',
      exec: 'providerPortal',
      startVUs: 0,
      startTime: '20s',
      stages: profileStages.portal,
      gracefulRampDown: '15s',
    },

    auth_flow: {
      executor: 'ramping-vus',
      exec: 'authFlow',
      startVUs: 0,
      startTime: '10s',
      stages: profileStages.authFlow,
      gracefulRampDown: '20s',
    },
  },

  thresholds: {
    // Global
    'http_req_duration':                                   ['p(95)<2000', 'p(99)<5000'],
    // Per-scenario
    'http_req_duration{scenario:browsing}':                ['p(95)<600',  'p(99)<1500'],
    'http_req_duration{scenario:mobile_app}':              ['p(95)<1000', 'p(99)<2500'],
    'http_req_duration{scenario:landing}':                 ['p(95)<600',  'p(99)<1500'],
    'http_req_duration{scenario:authenticated_reads}':     ['p(95)<800',  'p(99)<2000'],
    'http_req_duration{scenario:booking_writes}':          ['p(95)<2500', 'p(99)<5000'],
    'http_req_duration{scenario:admin_dashboard}':         ['p(95)<1500', 'p(99)<4000'],
    'http_req_duration{scenario:admin_operations}':        ['p(95)<2000', 'p(99)<5000'],
    'http_req_duration{scenario:provider_portal}':         ['p(95)<1200', 'p(99)<3000'],
    'http_req_duration{scenario:auth_flow}':               ['p(95)<1000', 'p(99)<2000'],
    // Custom trend thresholds
    'booking_create_ms':  ['p(95)<2500'],
    'auth_login_ms':      ['p(95)<800'],
    'dashboard_load_ms':  ['p(95)<2000'],
    'mobile_session_ms':  ['p(95)<1200'],
    'portal_req_ms':      ['p(95)<1500'],
    'landing_req_ms':     ['p(95)<600'],
    // Error budget – may be exceeded under extreme load, that's acceptable
    'server_errors':      ['count<50'],
  },

  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'],
};

// ── Static test data ───────────────────────────────────────────────────────────

const CONTENT_SLUGS   = ['about', 'terms', 'privacy', 'faq', 'contact', 'help', 'cookies', 'how-it-works', 'careers', 'blog'];
const SEARCH_TERMS    = ['hotel', 'resort', 'spa', 'restaurant', 'tour', 'chalet', 'villa', 'apartment', 'beach', 'mountain', 'pool', 'suite', 'boutique', 'camp', 'lodge', 'hostel', 'retreat', 'safari'];
const CURRENCIES      = ['USD', 'EUR', 'GBP', 'SAR', 'AED', 'TRY', 'EGP', 'JOD'];
const PAYMENT_METHODS = ['CREDIT_CARD', 'DEBIT_CARD', 'CASH'];
const SERVICE_TYPES   = ['HOTEL', 'RESORT', 'RESTAURANT', 'TAXI', 'TRIP'];

const IOS_UA     = 'Ziyara/3.2.1 (iPhone; iOS 17.5; Scale/3.00)';
const ANDROID_UA = 'Ziyara/3.2.1 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36';

// ── Helpers ────────────────────────────────────────────────────────────────────

const JSON_H = { 'Content-Type': 'application/json' };

function bearer(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

function mobileBearer(token) {
  const isIos = __VU % 2 === 0;
  return {
    'Content-Type':  'application/json',
    Authorization:   `Bearer ${token}`,
    'User-Agent':    isIos ? IOS_UA : ANDROID_UA,
    'X-App-Version': '3.2.1',
    'X-Platform':    isIos ? 'ios' : 'android',
  };
}

function doLogin(email, password) {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password, rememberMe: false }),
    { headers: JSON_H, tags: { name: 'setup:login' } },
  );
  if (res.status !== 200) return null;
  return res.json('data.accessToken') || null;
}

function assess(res, label, expectedStatuses) {
  const expected = expectedStatuses || [200];
  if (res.status >= 500) serverErrors.add(1);
  if (res.status === 429) rateLimited.add(1);
  check(res, {
    [`${label} ok`]:      (r) => expected.includes(r.status),
    [`${label} not 5xx`]: (r) => r.status < 500,
    [`${label} <5s`]:     (r) => r.timings.duration < 5000,
  });
}

function poolToken(pool) {
  if (!pool || pool.length === 0) return null;
  return pool[__VU % pool.length];
}

function coin()         { return rndInt(0, 1) === 0; }
function oneIn(n)       { return rndInt(0, n - 1) === 0; }
function pick(arr)      { return (!arr || arr.length === 0) ? null : arr[rndInt(0, arr.length - 1)]; }
function rndInt(mn, mx) { return Math.floor(Math.random() * (mx - mn + 1)) + mn; }
function rnd(mn, mx)    { return Math.random() * (mx - mn) + mn; }

function futureDate(days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().split('T')[0];
}

function buildTextSummary(data) {
  const m   = data.metrics;
  const p   = (metric, pct) => { const v = m[metric]?.values?.[`p(${pct})`]; return v !== undefined ? `${Math.round(v)}ms` : 'N/A'; };
  const cnt = (metric) => String(m[metric]?.values?.count ?? 0);
  const rt  = (metric) => { const v = m[metric]?.values?.rate; return v !== undefined ? `${(v * 100).toFixed(1)}%` : 'N/A'; };

  const W  = 58;
  const hr = '═'.repeat(W);
  const row = (label, value) => { const inner = `  ${label}: ${value}`; return `║${inner.padEnd(W)}║`; };

  return [
    '',
    `╔${hr}╗`,
    row(`Ziyara k6  profile: ${PROFILE}`, ''),
    `╠${hr}╣`,
    row('Total requests       ', cnt('http_reqs')),
    row('Failed %             ', rt('http_req_failed')),
    row('Server errors (5xx)  ', cnt('server_errors')),
    row('Rate limited  (429)  ', cnt('rate_limited')),
    row('Bookings created     ', cnt('bookings_created')),
    row('Token refreshes      ', cnt('token_refreshed')),
    row('Mobile requests      ', cnt('mobile_requests')),
    row('Admin dash hits      ', cnt('admin_dash_hits')),
    row('Admin ops hits       ', cnt('admin_ops_hits')),
    row('Portal hits          ', cnt('portal_hits')),
    row('Landing hits         ', cnt('landing_hits')),
    `╠${hr}╣`,
    row('Overall    p95       ', p('http_req_duration', 95)),
    row('Overall    p99       ', p('http_req_duration', 99)),
    row('Login      p95       ', p('auth_login_ms', 95)),
    row('Booking    p95       ', p('booking_create_ms', 95)),
    row('Dashboard  p95       ', p('dashboard_load_ms', 95)),
    row('Mobile     p95       ', p('mobile_session_ms', 95)),
    row('Portal     p95       ', p('portal_req_ms', 95)),
    row('Landing    p95       ', p('landing_req_ms', 95)),
    `╚${hr}╝`,
    '',
  ].join('\n');
}

// ── Setup ──────────────────────────────────────────────────────────────────────

export function setup() {
  const adminToken = doLogin(ADMIN_EMAIL, ADMIN_PASS);
  if (!adminToken) {
    fail(
      `Setup aborted: cannot login as ${ADMIN_EMAIL}. ` +
      `Check credentials, container health, and BASE_URL=${BASE_URL}`,
    );
  }

  // Build a diverse token pool — rotate through all available accounts so no
  // single account exceeds the 40-req/min login rate limit during setup.
  // Admin is always account[0]; extra accounts fill the rest of the rotation.
  const tokenCount = PROFILE === 'extreme' ? 20 : 8;
  const allAccounts = [{ email: ADMIN_EMAIL, pass: ADMIN_PASS }, ...EXTRA_ACCOUNTS];
  const tokenPool = [adminToken];
  for (let i = 1; i < tokenCount; i++) {
    sleep(0.5);
    const acct = allAccounts[i % allAccounts.length];
    const t = doLogin(acct.email, acct.pass);
    if (t) tokenPool.push(t);
    // If extra account login fails (account not seeded), silently fall back to admin.
    else {
      const fallback = doLogin(ADMIN_EMAIL, ADMIN_PASS);
      if (fallback) tokenPool.push(fallback);
    }
  }

  // Collect ALL service IDs by paginating through every page (cap: 10 pages = 1000 services).
  // A single page=0 fetch means every scenario always hits the same one service —
  // pagination here ensures load is spread across the full catalogue.
  function fetchAllServiceIds(statusFilter) {
    var ids = [];
    for (var pg = 0; pg < 10; pg++) {
      if (pg > 0) sleep(0.3);
      var url = BASE_URL + '/services?page=' + pg + '&size=100' + (statusFilter ? '&status=' + statusFilter : '');
      var res = http.get(url, { headers: bearer(adminToken), tags: { name: 'setup:services' } });
      if (res.status !== 200) break;
      var content = res.json('data.content') || [];
      var pageIds = content.map(function(s) { return s.id; }).filter(Boolean);
      ids = ids.concat(pageIds);
      var totalPages = res.json('data.totalPages') || 1;
      if (pg + 1 >= totalPages || pageIds.length < 100) break;
    }
    return ids;
  }

  var serviceIds = fetchAllServiceIds('ACTIVE');

  if (serviceIds.length === 0) {
    serviceIds = fetchAllServiceIds(null);
    if (serviceIds.length > 0) {
      console.log('[setup] No ACTIVE services — using ' + serviceIds.length + ' service(s) of any status');
    }
  }

  // Provider IDs — paginate to collect all providers, not just the first page
  var providerIds = [];
  for (var ppg = 0; ppg < 5; ppg++) {
    if (ppg > 0) sleep(0.2);
    var provRes = http.get(BASE_URL + '/providers?page=' + ppg + '&size=100', {
      headers: bearer(adminToken),
      tags: { name: 'setup:providers' },
    });
    if (provRes.status !== 200) break;
    var provContent = provRes.json('data.content') || [];
    var provPageIds = provContent.map(function(p) { return p.id; }).filter(Boolean);
    providerIds = providerIds.concat(provPageIds);
    var provTotalPages = provRes.json('data.totalPages') || 1;
    if (ppg + 1 >= provTotalPages || provPageIds.length < 100) break;
  }

  // Existing booking IDs (for read scenarios that need real IDs)
  const bkgRes = http.get(`${BASE_URL}/bookings/admin?page=0&size=50`, {
    headers: bearer(adminToken),
    tags: { name: 'setup:bookings' },
  });
  const bookingIds = bkgRes.status === 200
    ? (bkgRes.json('data.content') || []).map((b) => b.id).filter(Boolean)
    : [];

  // Provider portal token pool (optional — 10 tokens for extreme, 5 otherwise)
  let providerTokenPool = [];
  if (PROVIDER_EMAIL && PROVIDER_PASS) {
    const provTokenCount = PROFILE === 'extreme' ? 10 : 5;
    sleep(1);
    const pt = doLogin(PROVIDER_EMAIL, PROVIDER_PASS);
    if (pt) {
      providerTokenPool = [pt];
      for (let i = 1; i < provTokenCount; i++) {
        sleep(0.7);
        const t = doLogin(PROVIDER_EMAIL, PROVIDER_PASS);
        if (t) providerTokenPool.push(t);
      }
    }
  }

  console.log(
    `[setup] profile=${PROFILE}  adminTokens=${tokenPool.length}` +
    `  providerTokens=${providerTokenPool.length}` +
    `  services=${serviceIds.length}  providers=${providerIds.length}` +
    `  bookings=${bookingIds.length}  base=${BASE_URL}`,
  );

  return { tokenPool, providerTokenPool, serviceIds, providerIds, bookingIds };
}

// ── Scenario: Anonymous web browsing ──────────────────────────────────────────

export function browsing() {
  group('browsing', () => {
    // Paginated listing — random page so not all VUs hammer page 0
    const listRes = http.get(
      `${BASE_URL}/services?page=${rndInt(0, 8)}&size=20`,
      { tags: { name: 'services:list' } },
    );
    assess(listRes, 'services:list');
    sleep(rnd(0.4, 1.2));

    // Full-text search (jOOQ path)
    const searchRes = http.get(
      `${BASE_URL}/services/search?q=${pick(SEARCH_TERMS)}&page=${rndInt(0, 3)}&size=12`,
      { tags: { name: 'services:search' } },
    );
    assess(searchRes, 'services:search');
    sleep(rnd(0.3, 0.9));

    // Filter by service type
    if (coin()) {
      const typeRes = http.get(
        `${BASE_URL}/services?type=${pick(SERVICE_TYPES)}&page=0&size=20`,
        { tags: { name: 'services:filter-type' } },
      );
      assess(typeRes, 'services:filter-type');
      sleep(rnd(0.2, 0.6));
    }

    // Infinite scroll — deeper pagination
    if (coin()) {
      const deepRes = http.get(
        `${BASE_URL}/services?page=${rndInt(3, 12)}&size=20`,
        { tags: { name: 'services:paginate' } },
      );
      assess(deepRes, 'services:paginate');
      sleep(rnd(0.2, 0.5));
    }

    // Currency rates widget (shown on every page)
    if (oneIn(3)) {
      assess(
        http.get(`${BASE_URL}/currency/rates`, { tags: { name: 'browsing:currency' } }),
        'browsing:currency',
      );
      sleep(rnd(0.1, 0.3));
    }
  });

  sleep(rnd(0.5, 2.5));
}

// ── Scenario: Mobile app session ──────────────────────────────────────────────

export function mobileApp(data) {
  const token = poolToken(data.tokenPool);
  if (!token) { sleep(2); return; }
  const h  = mobileBearer(token);
  const t0 = Date.now();

  group('mobile_app', () => {
    // App open: fetch home feed (small page for mobile viewport)
    assess(
      http.get(`${BASE_URL}/services?page=0&size=10`, { headers: h, tags: { name: 'mobile:home' } }),
      'mobile:home',
    );
    sleep(rnd(0.5, 1.5));

    // Notification badge poll (the app does this on every resume)
    assess(
      http.get(`${BASE_URL}/notifications/me/unread-count`, { headers: h, tags: { name: 'mobile:unread-count' } }),
      'mobile:unread-count',
    );
    sleep(rnd(0.2, 0.5));

    // Currency rates (shown in service price cards)
    if (coin()) {
      http.get(`${BASE_URL}/currency/rates`, { headers: h, tags: { name: 'mobile:currency-rates' } });
      sleep(rnd(0.1, 0.3));
    }

    // Search
    assess(
      http.get(
        `${BASE_URL}/services/search?q=${pick(SEARCH_TERMS)}&page=0&size=8`,
        { headers: h, tags: { name: 'mobile:search' } },
      ),
      'mobile:search',
    );
    sleep(rnd(0.5, 1.5));

    // Service detail view
    const svcId = pick(data.serviceIds);
    if (svcId) {
      assess(
        http.get(`${BASE_URL}/services/${svcId}`, { headers: h, tags: { name: 'mobile:service-detail' } }),
        'mobile:service-detail',
      );
      sleep(rnd(0.8, 2.5));

      // Images carousel (always loaded on detail screen)
      http.get(`${BASE_URL}/services/${svcId}/images`, { headers: h, tags: { name: 'mobile:service-images' } });
      sleep(rnd(0.3, 0.8));

      // Availability calendar (user taps date picker)
      if (coin()) {
        http.get(`${BASE_URL}/services/${svcId}/availability`, { headers: h, tags: { name: 'mobile:availability' } });
        sleep(rnd(0.3, 0.8));
      }

      // Rooms (hotel) / Menu (restaurant) tab
      if (oneIn(3)) {
        http.get(`${BASE_URL}/services/${svcId}/rooms`, { headers: h, tags: { name: 'mobile:rooms' } });
        sleep(rnd(0.2, 0.5));
      } else if (oneIn(3)) {
        http.get(`${BASE_URL}/services/${svcId}/menu`, { headers: h, tags: { name: 'mobile:menu' } });
        sleep(rnd(0.2, 0.5));
      }

      // Pricing preview (user selects dates in checkout)
      if (coin()) {
        const checkIn  = futureDate(rndInt(7, 45));
        const checkOut = futureDate(rndInt(46, 52));
        http.post(
          `${BASE_URL}/pricing/preview`,
          JSON.stringify({ serviceId: svcId, checkInDate: checkIn, checkOutDate: checkOut, guests: rndInt(1, 3), rooms: 1 }),
          { headers: h, tags: { name: 'mobile:pricing-preview' } },
        );
        sleep(rnd(0.5, 1.5));

        // ~30% of users who reach pricing actually create a booking
        if (oneIn(3)) {
          const createRes = http.post(
            `${BASE_URL}/bookings`,
            JSON.stringify({
              serviceId:     svcId,
              checkInDate:   checkIn,
              checkOutDate:  checkOut,
              guests:        rndInt(1, 4),
              rooms:         1,
              currency:      pick(CURRENCIES),
              paymentMethod: pick(PAYMENT_METHODS),
            }),
            { headers: h, tags: { name: 'mobile:booking-create' } },
          );
          if (createRes.status === 201) {
            bookingsCreated.add(1);
            const bid = createRes.json('data.id');
            if (bid) {
              sleep(rnd(0.5, 1.0));
              http.get(`${BASE_URL}/bookings/${bid}`, { headers: h, tags: { name: 'mobile:booking-confirm' } });
            }
          } else if (createRes.status === 429) {
            rateLimited.add(1);
          } else if (createRes.status >= 500) {
            serverErrors.add(1);
          }
          sleep(rnd(1.0, 2.5));
        }
      }
    }

    // Trip history tab (my bookings)
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/bookings/my?page=0&size=5`, { headers: h, tags: { name: 'mobile:my-bookings' } }),
        'mobile:my-bookings',
      );
      sleep(rnd(0.3, 0.8));

      // Tap into a specific booking
      const bid = pick(data.bookingIds);
      if (bid) {
        http.get(`${BASE_URL}/bookings/${bid}`, { headers: h, tags: { name: 'mobile:booking-detail' } });
        sleep(rnd(0.3, 0.8));
      }
    }

    // Notification inbox (tapping bell icon)
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/notifications/me?page=0&size=10`, { headers: h, tags: { name: 'mobile:notifications' } }),
        'mobile:notifications',
      );
      sleep(rnd(0.3, 0.8));

      if (coin()) {
        http.post(`${BASE_URL}/notifications/read-all`, null, { headers: h, tags: { name: 'mobile:mark-all-read' } });
      }
    }

    // Profile tab (occasionally viewed)
    if (oneIn(5)) {
      assess(
        http.get(`${BASE_URL}/users/me`, { headers: h, tags: { name: 'mobile:profile' } }),
        'mobile:profile',
      );
      sleep(rnd(0.3, 0.8));
    }

    // FCM token registration (happens on app start ~10% of sessions due to token rotation)
    if (oneIn(10)) {
      http.post(
        `${BASE_URL}/users/me/fcm-token`,
        JSON.stringify({
          token:    `fcm_${__VU}_${Date.now()}`,
          platform: __VU % 2 === 0 ? 'IOS' : 'ANDROID',
        }),
        { headers: h, tags: { name: 'mobile:fcm-register' } },
      );
    }

    // Delivery tracking (active taxi/delivery bookings)
    const trackBid = pick(data.bookingIds);
    if (trackBid && oneIn(8)) {
      http.get(`${BASE_URL}/map/delivery/${trackBid}`, { headers: h, tags: { name: 'mobile:delivery-track' } });
    }

    mobileRequests.add(1);
  });

  mobileLatency.add(Date.now() - t0);
  sleep(rnd(1.0, 3.5));
}

// ── Scenario: Public landing and detail pages ──────────────────────────────────

export function landingPages(data) {
  const t0 = Date.now();

  group('landing', () => {
    // Static content page (about, terms, FAQ…)
    assess(
      http.get(`${BASE_URL}/content-pages/${pick(CONTENT_SLUGS)}`, { tags: { name: 'landing:content-page' } }),
      'landing:content-page',
      [200, 404], // 404 acceptable if slug not yet seeded
    );
    sleep(rnd(0.4, 1.0));

    // Currency rates (shown in pricing widgets across all pages)
    assess(
      http.get(`${BASE_URL}/currency/rates`, { tags: { name: 'landing:currency-rates' } }),
      'landing:currency-rates',
    );
    sleep(rnd(0.1, 0.3));

    // Live currency conversion (user selects a different display currency)
    const from = pick(CURRENCIES);
    const to   = pick(CURRENCIES.filter((c) => c !== from));
    assess(
      http.get(
        `${BASE_URL}/currency/convert?from=${from}&to=${to}&amount=${rndInt(50, 5000)}`,
        { tags: { name: 'landing:currency-convert' } },
      ),
      'landing:currency-convert',
    );
    sleep(rnd(0.1, 0.4));

    // Service listing (category/search landing page)
    if (coin()) {
      assess(
        http.get(
          `${BASE_URL}/services?type=${pick(SERVICE_TYPES)}&page=0&size=12`,
          { tags: { name: 'landing:category-list' } },
        ),
        'landing:category-list',
      );
      sleep(rnd(0.3, 0.8));
    }

    // Service detail page
    const svcId = pick(data.serviceIds);
    if (svcId) {
      assess(
        http.get(`${BASE_URL}/services/${svcId}`, { tags: { name: 'landing:service-detail' } }),
        'landing:service-detail',
      );
      sleep(rnd(0.5, 1.5));

      // Images gallery
      if (coin()) {
        http.get(`${BASE_URL}/services/${svcId}/images`, { tags: { name: 'landing:images' } });
        sleep(rnd(0.2, 0.5));
      }

      // Availability calendar (date picker interaction)
      if (coin()) {
        http.get(`${BASE_URL}/services/${svcId}/availability`, { tags: { name: 'landing:availability' } });
        sleep(rnd(0.2, 0.5));
      }

      // Rooms tab (hotels)
      if (oneIn(3)) {
        http.get(`${BASE_URL}/services/${svcId}/rooms`, { tags: { name: 'landing:rooms' } });
        sleep(rnd(0.2, 0.5));
      }

      // Menu tab (restaurants)
      if (oneIn(3)) {
        http.get(`${BASE_URL}/services/${svcId}/menu`, { tags: { name: 'landing:menu' } });
        sleep(rnd(0.2, 0.5));
      }

      // Pricing preview (anonymous — shows price before requiring login)
      if (coin()) {
        http.post(
          `${BASE_URL}/pricing/preview`,
          JSON.stringify({
            serviceId:    svcId,
            checkInDate:  futureDate(rndInt(7, 60)),
            checkOutDate: futureDate(rndInt(61, 67)),
            guests:       rndInt(1, 4),
            rooms:        1,
          }),
          { headers: JSON_H, tags: { name: 'landing:pricing-preview' } },
        );
        sleep(rnd(0.5, 1.5));
      }
    }

    // Public contact form (very low frequency)
    if (oneIn(25)) {
      http.post(
        `${BASE_URL}/public/contact`,
        JSON.stringify({
          name:    `Load Test User ${__VU}`,
          email:   `loadtest${__VU}@example.invalid`,
          subject: 'Test inquiry',
          message: 'This is an automated load test contact form submission.',
        }),
        { headers: JSON_H, tags: { name: 'landing:contact-form' } },
      );
    }
  });

  landingLatency.add(Date.now() - t0);
  landingHits.add(1);
  sleep(rnd(0.5, 2.0));
}

// ── Scenario: Authenticated web reads ─────────────────────────────────────────

export function authenticatedReads(data) {
  const token = poolToken(data.tokenPool);
  if (!token) { sleep(2); return; }
  const h = bearer(token);

  group('authenticated_reads', () => {
    // User profile
    assess(
      http.get(`${BASE_URL}/users/me`, { headers: h, tags: { name: 'users:me' } }),
      'users:me',
    );
    sleep(rnd(0.2, 0.6));

    // My bookings list
    assess(
      http.get(`${BASE_URL}/bookings/my?page=0&size=10`, { headers: h, tags: { name: 'bookings:my' } }),
      'bookings:my',
    );
    sleep(rnd(0.3, 0.8));

    // Open a booking from history
    const bid = pick(data.bookingIds);
    if (bid) {
      http.get(`${BASE_URL}/bookings/${bid}`, { headers: h, tags: { name: 'bookings:detail' } });
      sleep(rnd(0.3, 0.8));
    }

    // Notification inbox
    assess(
      http.get(`${BASE_URL}/notifications/me?page=0&size=20`, { headers: h, tags: { name: 'notifications:list' } }),
      'notifications:list',
    );
    sleep(rnd(0.2, 0.5));

    // Unread count badge
    assess(
      http.get(`${BASE_URL}/notifications/me/unread-count`, { headers: h, tags: { name: 'notifications:unread' } }),
      'notifications:unread',
    );
    sleep(rnd(0.1, 0.3));

    // Permissions (Redis-cached — fast)
    assess(
      http.get(`${BASE_URL}/users/me/permissions`, { headers: h, tags: { name: 'users:permissions' } }),
      'users:permissions',
    );
    sleep(rnd(0.2, 0.5));

    // Sidebar navigation (cached per role)
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/users/me/navigation`, { headers: h, tags: { name: 'users:navigation' } }),
        'users:navigation',
      );
      sleep(rnd(0.1, 0.3));
    }

    // Service catalog (logged-in user browsing)
    assess(
      http.get(`${BASE_URL}/services?page=${rndInt(0, 5)}&size=20`, { headers: h, tags: { name: 'services:authed' } }),
      'services:authed',
    );
    sleep(rnd(0.3, 0.8));

    // Discount validation (coupon entry in checkout)
    if (oneIn(6)) {
      http.post(
        `${BASE_URL}/discounts/validate`,
        JSON.stringify({ code: `SAVE${rndInt(10, 99)}`, serviceId: pick(data.serviceIds) }),
        { headers: h, tags: { name: 'discounts:validate' } },
      );
      sleep(rnd(0.2, 0.5));
    }

    // GDPR data export list (rarely accessed)
    if (oneIn(20)) {
      http.get(`${BASE_URL}/users/me/data-exports`, { headers: h, tags: { name: 'users:data-exports' } });
    }

    // Consent preferences (GDPR)
    if (oneIn(15)) {
      http.get(`${BASE_URL}/users/me/consents`, { headers: h, tags: { name: 'users:consents' } });
    }
  });

  sleep(rnd(1.0, 3.0));
}

// ── Scenario: Booking writes (checkout funnel) ─────────────────────────────────

export function bookingWrites(data) {
  const token = poolToken(data.tokenPool);
  if (!token) { sleep(2); return; }

  const svcId = pick(data.serviceIds);
  if (!svcId) {
    http.get(`${BASE_URL}/services?page=0&size=5`, { headers: bearer(token), tags: { name: 'bookings:fallback-read' } });
    sleep(3);
    return;
  }

  const h        = bearer(token);
  const checkIn  = futureDate(rndInt(7, 60));
  const checkOut = futureDate(rndInt(61, 67));

  // Service detail (user reads the listing before booking)
  http.get(`${BASE_URL}/services/${svcId}`, { headers: h, tags: { name: 'bookings:pre-detail' } });
  sleep(rnd(0.8, 2.0));

  // Availability check
  http.get(`${BASE_URL}/services/${svcId}/availability`, { headers: h, tags: { name: 'bookings:availability' } });
  sleep(rnd(0.3, 0.8));

  // Pricing preview (always shown in checkout)
  http.post(
    `${BASE_URL}/pricing/preview`,
    JSON.stringify({ serviceId: svcId, checkInDate: checkIn, checkOutDate: checkOut, guests: rndInt(1, 3), rooms: 1 }),
    { headers: h, tags: { name: 'bookings:pricing-preview' } },
  );
  sleep(rnd(0.5, 1.5));

  // Coupon entry (~25% of users try a discount code)
  if (oneIn(4)) {
    http.post(
      `${BASE_URL}/discounts/validate`,
      JSON.stringify({ code: `PROMO${rndInt(10, 50)}`, serviceId: svcId }),
      { headers: h, tags: { name: 'bookings:discount-validate' } },
    );
    sleep(rnd(0.3, 0.8));
  }

  // Create booking
  const t0 = Date.now();
  const createRes = http.post(
    `${BASE_URL}/bookings`,
    JSON.stringify({
      serviceId:     svcId,
      checkInDate:   checkIn,
      checkOutDate:  checkOut,
      guests:        rndInt(1, 4),
      rooms:         rndInt(1, 2),
      currency:      pick(CURRENCIES),
      paymentMethod: pick(PAYMENT_METHODS),
    }),
    { headers: h, tags: { name: 'bookings:create' } },
  );
  bookingLatency.add(Date.now() - t0);

  if (createRes.status === 201) {
    check(createRes, {
      'booking has id':        (r) => Boolean(r.json('data.id')),
      'booking has reference': (r) => Boolean(r.json('data.reference')),
    });
    bookingsCreated.add(1);

    const bid = createRes.json('data.id');
    if (bid) {
      sleep(rnd(0.3, 0.8));

      // Confirmation page
      assess(
        http.get(`${BASE_URL}/bookings/${bid}`, { headers: h, tags: { name: 'bookings:confirm' } }),
        'bookings:confirm',
      );
      sleep(rnd(0.5, 1.0));

      // Voucher download (~60% of confirmed bookings)
      if (coin()) {
        http.get(`${BASE_URL}/bookings/${bid}/voucher`, { headers: h, tags: { name: 'bookings:voucher' } });
        sleep(rnd(0.2, 0.5));
      }

      // Payment initiation (online payment flow)
      if (coin()) {
        http.post(
          `${BASE_URL}/payments/initiate`,
          JSON.stringify({
            bookingId: bid,
            amount:    rnd(100, 2000),
            currency:  'USD',
            method:    pick(PAYMENT_METHODS),
          }),
          { headers: h, tags: { name: 'bookings:payment-initiate' } },
        );
      }
    }
  } else if (createRes.status === 429) {
    rateLimited.add(1);
    sleep(rnd(3, 7));
  } else if (createRes.status >= 500) {
    serverErrors.add(1);
    check(createRes, { 'booking no 5xx': () => false });
  }
  // 400/409/422 (unavailable dates, capacity) are expected business-rule rejections

  sleep(rnd(2.0, 5.0));
}

// ── Scenario: Admin company dashboard ─────────────────────────────────────────

export function adminDashboard(data) {
  const token = poolToken(data.tokenPool);
  if (!token) { sleep(2); return; }
  const h  = bearer(token);
  const t0 = Date.now();

  group('admin_dashboard', () => {
    // Bootstrap — loads all widgets at once on initial page load (heaviest call)
    assess(
      http.get(`${BASE_URL}/dashboard/bootstrap`, { headers: h, tags: { name: 'dash:bootstrap' } }),
      'dash:bootstrap',
    );
    sleep(rnd(1.5, 3.0)); // admin reads the dashboard before clicking anything

    // KPI cards — re-fetched on manual refresh
    assess(
      http.get(`${BASE_URL}/dashboard/kpis`, { headers: h, tags: { name: 'dash:kpis' } }),
      'dash:kpis',
    );
    sleep(rnd(0.5, 1.5));

    // Live stats panel (lower TTL — 30s)
    assess(
      http.get(`${BASE_URL}/dashboard/live`, { headers: h, tags: { name: 'dash:live' } }),
      'dash:live',
    );
    sleep(rnd(0.3, 0.8));

    // Activity feed
    assess(
      http.get(`${BASE_URL}/dashboard/activity?limit=${rndInt(10, 25)}`, { headers: h, tags: { name: 'dash:activity' } }),
      'dash:activity',
    );
    sleep(rnd(0.3, 0.8));

    // Revenue chart
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/dashboard/revenue`, { headers: h, tags: { name: 'dash:revenue' } }),
        'dash:revenue',
      );
      sleep(rnd(0.3, 0.8));
    }

    // Bookings chart
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/dashboard/bookings`, { headers: h, tags: { name: 'dash:bookings' } }),
        'dash:bookings',
      );
      sleep(rnd(0.3, 0.8));
    }

    // Customer stats
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/dashboard/customers`, { headers: h, tags: { name: 'dash:customers' } }),
        'dash:customers',
      );
      sleep(rnd(0.2, 0.6));
    }

    // Provider stats
    assess(
      http.get(`${BASE_URL}/dashboard/providers`, { headers: h, tags: { name: 'dash:providers' } }),
      'dash:providers',
    );
    sleep(rnd(0.2, 0.6));

    // Service health widget (2-min cache)
    assess(
      http.get(`${BASE_URL}/dashboard/service-health`, { headers: h, tags: { name: 'dash:service-health' } }),
      'dash:service-health',
    );
    sleep(rnd(0.2, 0.5));

    // Commission analysis (5-min cache)
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/dashboard/commission-analysis`, { headers: h, tags: { name: 'dash:commission' } }),
        'dash:commission',
      );
      sleep(rnd(0.2, 0.5));
    }

    // Payout summary (5-min cache)
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/dashboard/payouts`, { headers: h, tags: { name: 'dash:payouts' } }),
        'dash:payouts',
      );
      sleep(rnd(0.2, 0.5));
    }

    adminDashHits.add(1);
  });

  dashboardLatency.add(Date.now() - t0);
  sleep(rnd(3.0, 8.0)); // admins read charts for a while before refreshing
}

// ── Scenario: Admin operational work ──────────────────────────────────────────

export function adminOperations(data) {
  const token = poolToken(data.tokenPool);
  if (!token) { sleep(2); return; }
  const h        = bearer(token);
  const stream   = rndInt(0, 7);

  if (stream === 0) {
    // ── Complaints queue ────────────────────────────────────────────────────
    group('admin_ops:complaints', () => {
      const listRes = http.get(
        `${BASE_URL}/complaints?page=${rndInt(0, 3)}&size=20`,
        { headers: h, tags: { name: 'ops:complaints-list' } },
      );
      assess(listRes, 'ops:complaints-list');
      sleep(rnd(0.5, 1.5));

      const complaints = listRes.status === 200 ? (listRes.json('data.content') || []) : [];
      const c = pick(complaints);
      if (c && c.id) {
        http.get(`${BASE_URL}/complaints/${c.id}`, { headers: h, tags: { name: 'ops:complaint-detail' } });
        sleep(rnd(0.8, 2.0));
        http.get(`${BASE_URL}/complaints/${c.id}/comments`, { headers: h, tags: { name: 'ops:complaint-comments' } });
        sleep(rnd(0.3, 0.8));
      }
    });

  } else if (stream === 1) {
    // ── Internal tickets ────────────────────────────────────────────────────
    group('admin_ops:tickets', () => {
      const listRes = http.get(
        `${BASE_URL}/tickets?page=${rndInt(0, 3)}&size=20`,
        { headers: h, tags: { name: 'ops:tickets-list' } },
      );
      assess(listRes, 'ops:tickets-list');
      sleep(rnd(0.3, 0.8));

      http.get(`${BASE_URL}/tickets/stats`, { headers: h, tags: { name: 'ops:tickets-stats' } });
      sleep(rnd(0.2, 0.5));

      if (coin()) {
        http.get(`${BASE_URL}/tickets/overdue`, { headers: h, tags: { name: 'ops:tickets-overdue' } });
        sleep(rnd(0.2, 0.5));
      }

      const tickets = listRes.status === 200 ? (listRes.json('data.content') || []) : [];
      const t = pick(tickets);
      if (t && t.id) {
        http.get(`${BASE_URL}/tickets/${t.id}`, { headers: h, tags: { name: 'ops:ticket-detail' } });
        sleep(rnd(0.8, 2.0));
        http.get(`${BASE_URL}/tickets/${t.id}/comments`, { headers: h, tags: { name: 'ops:ticket-comments' } });
      }
    });

  } else if (stream === 2) {
    // ── Reports ─────────────────────────────────────────────────────────────
    group('admin_ops:reports', () => {
      const rptStart = futureDate(-30);
      const rptEnd   = futureDate(0);
      assess(
        http.get(`${BASE_URL}/reports/revenue?start=${rptStart}&end=${rptEnd}`, { headers: h, tags: { name: 'ops:report-revenue' } }),
        'ops:report-revenue',
      );
      sleep(rnd(0.8, 2.0));

      if (coin()) {
        assess(
          http.get(`${BASE_URL}/reports/bookings?start=${rptStart}&end=${rptEnd}`, { headers: h, tags: { name: 'ops:report-bookings' } }),
          'ops:report-bookings',
        );
        sleep(rnd(0.5, 1.0));
      }

      if (coin()) {
        assess(
          http.get(`${BASE_URL}/reports/analytics?start=${rptStart}&end=${rptEnd}`, { headers: h, tags: { name: 'ops:report-analytics' } }),
          'ops:report-analytics',
        );
        sleep(rnd(0.5, 1.0));
      }

      if (oneIn(4)) {
        http.get(
          `${BASE_URL}/reports/customer-search?q=${pick(SEARCH_TERMS)}`,
          { headers: h, tags: { name: 'ops:report-customer-search' } },
        );
      }
    });

  } else if (stream === 3) {
    // ── Payments & payouts ───────────────────────────────────────────────────
    group('admin_ops:payments', () => {
      assess(
        http.get(`${BASE_URL}/payments?page=${rndInt(0, 5)}&size=20`, { headers: h, tags: { name: 'ops:payments-list' } }),
        'ops:payments-list',
      );
      sleep(rnd(0.3, 0.8));

      if (coin()) {
        http.get(`${BASE_URL}/payments/summary`, { headers: h, tags: { name: 'ops:payments-summary' } });
        sleep(rnd(0.2, 0.5));
      }

      // Payouts list
      assess(
        http.get(`${BASE_URL}/admin/payouts?page=${rndInt(0, 3)}&size=20`, { headers: h, tags: { name: 'ops:payouts-list' } }),
        'ops:payouts-list',
      );
      sleep(rnd(0.3, 0.8));

      if (coin()) {
        http.get(`${BASE_URL}/admin/payouts/summary`, { headers: h, tags: { name: 'ops:payouts-summary' } });
        sleep(rnd(0.2, 0.5));
      }

      // Cash reconciliation queue
      if (coin()) {
        http.get(`${BASE_URL}/admin/cash/pending-reconciliation`, { headers: h, tags: { name: 'ops:cash-pending' } });
      }
    });

  } else if (stream === 4) {
    // ── User management ─────────────────────────────────────────────────────
    group('admin_ops:users', () => {
      assess(
        http.get(`${BASE_URL}/users?page=${rndInt(0, 5)}&size=20`, { headers: h, tags: { name: 'ops:users-list' } }),
        'ops:users-list',
      );
      sleep(rnd(0.3, 0.8));

      // Admin view of all bookings
      if (coin()) {
        assess(
          http.get(`${BASE_URL}/bookings/admin?page=${rndInt(0, 5)}&size=20`, { headers: h, tags: { name: 'ops:bookings-admin' } }),
          'ops:bookings-admin',
        );
        sleep(rnd(0.3, 0.8));
      }

      // Customer search
      if (coin()) {
        http.get(
          `${BASE_URL}/admin/super/customers/search?q=${pick(SEARCH_TERMS)}`,
          { headers: h, tags: { name: 'ops:customer-search' } },
        );
        sleep(rnd(0.2, 0.5));
      }

      // Login history for a specific user (when investigating an account)
      if (oneIn(5)) {
        const uid = rndInt(1, 100).toString();
        http.get(`${BASE_URL}/users/${uid}/login-history`, { headers: h, tags: { name: 'ops:user-login-history' } });
      }
    });

  } else if (stream === 5) {
    // ── Reviews moderation & discounts ──────────────────────────────────────
    group('admin_ops:reviews', () => {
      assess(
        http.get(`${BASE_URL}/reviews?page=${rndInt(0, 4)}&size=20`, { headers: h, tags: { name: 'ops:reviews-list' } }),
        'ops:reviews-list',
      );
      sleep(rnd(0.3, 0.8));

      const svcId = pick(data.serviceIds);
      if (svcId && coin()) {
        http.get(`${BASE_URL}/reviews/service/${svcId}`, { headers: h, tags: { name: 'ops:service-reviews' } });
        sleep(rnd(0.2, 0.6));
      }

      // Discounts management
      if (coin()) {
        assess(
          http.get(`${BASE_URL}/discounts?page=0&size=20`, { headers: h, tags: { name: 'ops:discounts-list' } }),
          'ops:discounts-list',
        );
        sleep(rnd(0.2, 0.5));
      }

      // Departments
      if (oneIn(4)) {
        http.get(`${BASE_URL}/departments`, { headers: h, tags: { name: 'ops:departments' } });
      }

      // Employees
      if (oneIn(4)) {
        http.get(`${BASE_URL}/employees?page=0&size=20`, { headers: h, tags: { name: 'ops:employees' } });
      }
    });

  } else if (stream === 6) {
    // ── Audit logs & compliance ──────────────────────────────────────────────
    group('admin_ops:audit', () => {
      assess(
        http.get(`${BASE_URL}/audit-logs?page=${rndInt(0, 3)}&size=20`, { headers: h, tags: { name: 'ops:audit-list' } }),
        'ops:audit-list',
      );
      sleep(rnd(0.5, 1.5));

      if (coin()) {
        http.get(`${BASE_URL}/audit-logs/deletions`, { headers: h, tags: { name: 'ops:audit-deletions' } });
        sleep(rnd(0.2, 0.5));
      }

      // Recently deleted items
      if (oneIn(4)) {
        http.get(`${BASE_URL}/admin/super/deleted/recent`, { headers: h, tags: { name: 'ops:deleted-recent' } });
      }

      // Roles & permissions catalogue (when investigating access issues)
      if (oneIn(5)) {
        http.get(`${BASE_URL}/roles`, { headers: h, tags: { name: 'ops:roles-list' } });
        sleep(rnd(0.2, 0.5));
        http.get(`${BASE_URL}/roles/permissions/catalogue`, { headers: h, tags: { name: 'ops:permissions-catalogue' } });
      }
    });

  } else {
    // ── Media submissions & subscriptions ────────────────────────────────────
    group('admin_ops:media', () => {
      assess(
        http.get(`${BASE_URL}/admin/media-submissions?page=0&size=20`, { headers: h, tags: { name: 'ops:media-queue' } }),
        'ops:media-queue',
      );
      sleep(rnd(0.3, 0.8));

      assess(
        http.get(`${BASE_URL}/admin/subscriptions?page=0&size=20`, { headers: h, tags: { name: 'ops:subscriptions' } }),
        'ops:subscriptions',
      );
      sleep(rnd(0.2, 0.5));

      // All provider support requests
      if (coin()) {
        http.get(`${BASE_URL}/portal/support-requests/all`, { headers: h, tags: { name: 'ops:support-all' } });
      }

      // Webhooks management
      if (oneIn(5)) {
        http.get(`${BASE_URL}/admin/webhooks`, { headers: h, tags: { name: 'ops:webhooks' } });
      }

      // System settings
      if (oneIn(6)) {
        http.get(`${BASE_URL}/admin/settings`, { headers: h, tags: { name: 'ops:system-settings' } });
      }
    });
  }

  adminOpsHits.add(1);
  sleep(rnd(2.5, 6.0)); // admins read content carefully before acting
}

// ── Scenario: Provider portal ──────────────────────────────────────────────────

export function providerPortal(data) {
  const pool  = data.providerTokenPool && data.providerTokenPool.length > 0
    ? data.providerTokenPool
    : data.tokenPool;
  const token = poolToken(pool);
  if (!token) { sleep(2); return; }
  const h  = bearer(token);
  const t0 = Date.now();

  group('provider_portal', () => {
    // Portal dashboard (requires PROVIDER_PORTAL permission)
    const dashRes = http.get(`${BASE_URL}/portal/dashboard`, { headers: h, tags: { name: 'portal:dashboard' } });
    assess(dashRes, 'portal:dashboard', [200, 403]);
    sleep(rnd(0.8, 2.0));

    if (dashRes.status !== 200) {
      // Admin-token fallback: hit admin KPIs so the VU stays productive
      http.get(`${BASE_URL}/dashboard/kpis`, { headers: h, tags: { name: 'portal:admin-kpis-fallback' } });
      sleep(rnd(0.5, 1.0));
      return;
    }

    // My services list (paginated)
    const svcRes = http.get(
      `${BASE_URL}/portal/services?page=${rndInt(0, 3)}&size=20`,
      { headers: h, tags: { name: 'portal:services-list' } },
    );
    assess(svcRes, 'portal:services-list');
    sleep(rnd(0.3, 0.8));

    // Open a service and view its media / menu
    const services = svcRes.status === 200 ? (svcRes.json('data.content') || []) : [];
    const svc = pick(services);
    if (svc && svc.id) {
      http.get(`${BASE_URL}/portal/services/${svc.id}/images`, { headers: h, tags: { name: 'portal:service-images' } });
      sleep(rnd(0.2, 0.5));

      if (coin()) {
        http.get(`${BASE_URL}/portal/services/${svc.id}/menu`, { headers: h, tags: { name: 'portal:service-menu' } });
        sleep(rnd(0.2, 0.5));
      }
    }

    // My bookings (paginated)
    assess(
      http.get(`${BASE_URL}/portal/bookings?page=${rndInt(0, 3)}&size=20`, { headers: h, tags: { name: 'portal:bookings' } }),
      'portal:bookings',
    );
    sleep(rnd(0.3, 0.8));

    // Earnings / revenue report
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/portal/earnings`, { headers: h, tags: { name: 'portal:earnings' } }),
        'portal:earnings',
      );
      sleep(rnd(0.5, 1.2));
    }

    // My provider profile (edit mode)
    if (coin()) {
      http.get(`${BASE_URL}/providers/me`, { headers: h, tags: { name: 'portal:provider-profile' } });
      sleep(rnd(0.3, 0.8));
    }

    // Staff management
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/portal/staff`, { headers: h, tags: { name: 'portal:staff-list' } }),
        'portal:staff-list',
      );
      sleep(rnd(0.2, 0.6));

      if (coin()) {
        http.get(`${BASE_URL}/portal/staff/linkable`, { headers: h, tags: { name: 'portal:staff-linkable' } });
        sleep(rnd(0.2, 0.5));
      }
    }

    // Cash collections (finance tab)
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/portal/cash/collections`, { headers: h, tags: { name: 'portal:cash-collections' } }),
        'portal:cash-collections',
      );
      sleep(rnd(0.3, 0.8));
    }

    // Daily cash sheet
    if (oneIn(3)) {
      http.get(`${BASE_URL}/portal/cash/daily-sheet`, { headers: h, tags: { name: 'portal:daily-sheet' } });
      sleep(rnd(0.2, 0.5));
    }

    // My support requests (help tab)
    if (coin()) {
      assess(
        http.get(`${BASE_URL}/portal/support-requests`, { headers: h, tags: { name: 'portal:support-requests' } }),
        'portal:support-requests',
      );
      sleep(rnd(0.2, 0.5));
    }

    // Map pins (service location overview)
    if (coin()) {
      http.get(`${BASE_URL}/portal/map/pins`, { headers: h, tags: { name: 'portal:map-pins' } });
      sleep(rnd(0.1, 0.3));
    }

    // Subscription info
    const provId = pick(data.providerIds);
    if (provId && oneIn(4)) {
      http.get(`${BASE_URL}/providers/${provId}/subscription`, { headers: h, tags: { name: 'portal:subscription' } });
      sleep(rnd(0.2, 0.5));
    }

    // Subscription plans (upgrade path)
    if (provId && oneIn(6)) {
      http.get(`${BASE_URL}/providers/${provId}/subscription/plans`, { headers: h, tags: { name: 'portal:sub-plans' } });
    }

    portalHits.add(1);
  });

  portalLatency.add(Date.now() - t0);
  sleep(rnd(1.5, 4.5));
}

// ── Scenario: Auth lifecycle ───────────────────────────────────────────────────

export function authFlow() {
  sleep(rnd(0, 6)); // stagger logins so they don't burst simultaneously

  group('auth_flow', () => {
    const t0 = Date.now();
    const loginRes = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASS, rememberMe: false }),
      { headers: JSON_H, tags: { name: 'auth:login' } },
    );
    authLatency.add(Date.now() - t0);

    if (loginRes.status === 429) {
      rateLimited.add(1);
      sleep(rnd(4, 10));
      return;
    }
    if (loginRes.status >= 500) serverErrors.add(1);

    const loginOk = check(loginRes, {
      'login 200':          (r) => r.status === 200,
      'login has token':    (r) => Boolean(r.json('data.accessToken')),
      'login latency <1s':  (r) => r.timings.duration < 1000,
    });
    if (!loginOk) { sleep(3); return; }

    const accessToken  = loginRes.json('data.accessToken');
    const refreshToken = loginRes.json('data.refreshToken');
    const h = bearer(accessToken);
    sleep(rnd(0.5, 1.5));

    // Read profile immediately after login
    check(
      http.get(`${BASE_URL}/users/me`, { headers: h, tags: { name: 'auth:me-post-login' } }),
      { 'profile 200': (r) => r.status === 200 },
    );
    sleep(rnd(0.3, 0.8));

    // Permissions + navigation (always fetched to build the sidebar)
    http.get(`${BASE_URL}/users/me/permissions`, { headers: h, tags: { name: 'auth:permissions' } });
    http.get(`${BASE_URL}/users/me/navigation`,  { headers: h, tags: { name: 'auth:navigation'  } });
    sleep(rnd(1.0, 3.0));

    // Token refresh (simulates a session that's been open for a while)
    if (refreshToken) {
      const refreshRes = http.post(
        `${BASE_URL}/auth/refresh`,
        null,
        { headers: { ...h, 'Refresh-Token': refreshToken }, tags: { name: 'auth:refresh' } },
      );
      check(refreshRes, { 'refresh 200': (r) => r.status === 200 });
      if (refreshRes.status === 200) tokenRefreshed.add(1);
      sleep(rnd(0.3, 0.8));
    }

    // Password change attempt (same password — expected 400/422)
    if (oneIn(10)) {
      http.post(
        `${BASE_URL}/users/me/change-password`,
        JSON.stringify({ currentPassword: ADMIN_PASS, newPassword: ADMIN_PASS }),
        { headers: h, tags: { name: 'auth:change-pw' } },
      );
      sleep(rnd(0.3, 0.8));
    }

    // Logout
    check(
      http.post(`${BASE_URL}/auth/logout`, null, { headers: h, tags: { name: 'auth:logout' } }),
      { 'logout 200': (r) => r.status === 200 },
    );
  });

  sleep(rnd(5, 20)); // users don't log back in immediately
}

// ── Teardown ───────────────────────────────────────────────────────────────────

export function teardown(data) {
  const allTokens = [...(data.tokenPool || []), ...(data.providerTokenPool || [])];
  for (const token of allTokens) {
    http.post(`${BASE_URL}/auth/logout`, null, {
      headers: bearer(token),
      tags: { name: 'teardown:logout' },
    });
    sleep(0.1);
  }
}

// ── Summary ────────────────────────────────────────────────────────────────────

export function handleSummary(data) {
  return {
    'results.json': JSON.stringify(data, null, 2),
    stdout: buildTextSummary(data),
  };
}

// ── Default export ─────────────────────────────────────────────────────────────

export default {
  options,
  setup,
  browsing,
  mobileApp,
  landingPages,
  authenticatedReads,
  bookingWrites,
  adminDashboard,
  adminOperations,
  providerPortal,
  authFlow,
  teardown,
  handleSummary,
};