CREATE TABLE IF NOT EXISTS web_content_pages (
    id UUID PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE,
    content_en JSONB NOT NULL DEFAULT '{}'::jsonb,
    content_ar JSONB NOT NULL DEFAULT '{}'::jsonb,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_web_content_pages_slug ON web_content_pages(slug);
CREATE INDEX IF NOT EXISTS idx_web_content_pages_published ON web_content_pages(published);

INSERT INTO web_content_pages (id, slug, content_en, content_ar, published)
VALUES
(
    '10fcbf6f-6d3f-4ed1-a4ee-a9dbd5f03d51',
    'home',
    '{
      "badge":"Business travel platform",
      "heroTitle":"A complete business website for modern travel operations",
      "heroBody":"Ziyara helps travel businesses unify stays, rides, experiences, and dining workflows with a reliable platform for teams and partners.",
      "searchTitle":"Search and compare offers from live inventory",
      "searchCta":"Search deals",
      "dealsTitle":"Your fastest way to compare curated business rates",
      "popularTitle":"Explore top destinations this week"
    }'::jsonb,
    '{
      "badge":"منصة أعمال للسفر",
      "heroTitle":"موقع أعمال متكامل لعمليات السفر الحديثة",
      "heroBody":"تساعدك زيارة على توحيد الإقامات والنقل والتجارب والمطاعم عبر منصة موثوقة للفرق والشركاء.",
      "searchTitle":"ابحث وقارن العروض من مخزون مباشر",
      "searchCta":"ابحث عن العروض",
      "dealsTitle":"أسرع طريقة لمقارنة الأسعار التجارية المختارة",
      "popularTitle":"استكشف الوجهات الأكثر طلبًا هذا الأسبوع"
    }'::jsonb,
    TRUE
),
(
    'f06c5aa4-1810-4e6c-beb5-9bd2f82a95f1',
    'about',
    '{
      "eyebrow":"Who we are",
      "title":"Built to simplify travel commerce for companies",
      "body":"From booking operations to partner collaboration, Ziyara gives teams one place to manage demand, improve service quality, and scale confidently."
    }'::jsonb,
    '{
      "eyebrow":"من نحن",
      "title":"نبني حلولًا تُبسّط تجارة السفر للشركات",
      "body":"من تشغيل الحجوزات إلى التعاون مع الشركاء، تمنح زيارة فرق العمل منصة واحدة لإدارة الطلب وتحسين جودة الخدمة والتوسع بثقة."
    }'::jsonb,
    TRUE
),
(
    'cb5dffdc-71b6-4987-a32a-45d8e2f8fc60',
    'services',
    '{
      "eyebrow":"Solutions",
      "title":"Core services for your business growth"
    }'::jsonb,
    '{
      "eyebrow":"حلولنا",
      "title":"خدمات أساسية لنمو أعمالك"
    }'::jsonb,
    TRUE
),
(
    '74a04f7f-c17a-4d89-a57f-0de6d1088e54',
    'contact',
    '{
      "eyebrow":"Contact",
      "title":"Talk with our team",
      "body":"Share your goals and our team will reach out with a tailored walkthrough."
    }'::jsonb,
    '{
      "eyebrow":"تواصل معنا",
      "title":"تحدث مع فريقنا",
      "body":"شارك أهدافك وسيتواصل فريقنا معك بجولة تعريفية مناسبة."
    }'::jsonb,
    TRUE
),
(
    'af41c126-c5eb-4b5f-851f-f634fcff62c3',
    'faq',
    '{
      "eyebrow":"Frequently asked questions",
      "title":"Answers for teams evaluating Ziyara"
    }'::jsonb,
    '{
      "eyebrow":"الأسئلة الشائعة",
      "title":"إجابات للفرق التي تقيّم زيارة"
    }'::jsonb,
    TRUE
),
(
    '1f219748-4b35-46f9-9d4a-8ef09f54457d',
    'privacy',
    '{
      "title":"Privacy policy",
      "intro":"This page explains how we collect, use, and protect information when you interact with Ziyara pages and services."
    }'::jsonb,
    '{
      "title":"سياسة الخصوصية",
      "intro":"توضح هذه الصفحة كيف نجمع البيانات ونستخدمها ونحميها عند تفاعلك مع صفحات وخدمات زيارة."
    }'::jsonb,
    TRUE
),
(
    'f714d623-b314-4b9a-9e03-13f621fa4fcb',
    'terms',
    '{
      "title":"Terms and conditions",
      "intro":"These terms govern use of Ziyara services, partner tools, and public website content."
    }'::jsonb,
    '{
      "title":"الشروط والأحكام",
      "intro":"تنظم هذه الشروط استخدام خدمات زيارة وأدوات الشركاء ومحتوى الموقع العام."
    }'::jsonb,
    TRUE
)
ON CONFLICT (slug) DO NOTHING;
