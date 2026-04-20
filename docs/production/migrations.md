# Production migration policy

This project does not ship a backend database, so there are no schema/data migrations.

For each release, treat the following as the migration gate:

1. Bump `versionCode` and `versionName` in `app/build.gradle`.
2. Verify module metadata remains valid in `app/src/main/AndroidManifest.xml`.
3. Confirm hook signatures still match target Android APIs.
4. Run prod-like smoke tests before staged rollout.
5. Keep previous signed APK available for rollback.
