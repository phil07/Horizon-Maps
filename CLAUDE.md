# Working with the owner of this repo

- Talk to the owner in German.
- Before building a feature, ask questions first, as multiple-choice questions with suggested answers to pick from (the AskUserQuestion tool), not as open questions in the text. Ask more of these rather than fewer.
- Do not use agents (subagents) for anything; do all the work directly.
- Push only to `main`, never to other branches.
- There is no local Android build (Gradle/Maven downloads are blocked). CI (GitHub Actions, release-apk.yml) is the only compiler, so check its result after each push.
- Never commit the signing keystore or any API key or token.
- Changelog: with every push to `main`, add the user-facing changes to the `## Unreleased` section of CHANGELOG.md (German, with an `EN:` line each), and empty the section of the entries that were already published in the build before. The app's "What's new" card shows only what is in that section.
