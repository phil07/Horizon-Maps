# Working with the owner of this repo

- Talk to the owner in German.
- Before building a feature, ask questions first, as multiple-choice questions with suggested answers to pick from (the AskUserQuestion tool), not as open questions in the text. Ask more of these rather than fewer.
- Plan bigger features with several agents: each agent takes one part, then each agent reviews another agent's part, and the bugs found are fixed before pushing.
- Push only to `main`, never to other branches.
- There is no local Android build (Gradle/Maven downloads are blocked). CI (GitHub Actions, release-apk.yml) is the only compiler, so check its result after each push.
- Never commit the signing keystore or any API key or token.
