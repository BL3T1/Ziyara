# Snyk CI baseline (optional “new findings only” gating)

This repo now runs Snyk in CI via `.github/workflows/snyk-security.yml`.

At the moment the workflow fails the build on any **High/Critical** issues it finds.
If you want “new findings only” (fail only when issues are introduced after a baseline),
Snyk provides a *baseline* mechanism in the CLI.

## How to introduce a baseline

1. Run Snyk locally with a baseline output flag:
   - `snyk test --help` and `snyk code test --help` to confirm the exact options for your installed Snyk CLI version.
2. Commit the generated baseline file(s) (for example, `snyk-baseline.json`) to the repo.
3. Update the workflow to pass the baseline file back into the scan commands (again, check `--help` for the correct flag names).

## Notes

- Baseline flags and their names can vary slightly by Snyk CLI version; treat this doc as a checklist rather than a copy/paste command.
- Prefer making security fixes first; use baseline only as a temporary bridge while you clean up existing issues.

