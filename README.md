# Springfield Archive

A software project developed as a **public academic delivery and professional portfolio piece**.

Springfield Archive is being designed and built incrementally, with an emphasis on code quality, security, reproducibility and good engineering practices. This repository is the project's source of truth and is developed in the open.

> **Status: early stage.** No application code exists yet. This repository currently contains only the project bootstrap (license, ignore rules and documentation). Features will be documented here only once they exist and are verified.

## What this project is about

Springfield Archive explores the design of a digital archive: organizing, describing and presenting cataloged material through a maintainable web application. The domain scope and feature set will be defined and documented as the project progresses — nothing here is vaporware, and nothing is documented ahead of implementation.

## Disclaimer

Springfield Archive is an **unofficial fan and academic project**, developed for educational and portfolio purposes. It is **not affiliated with, authorized, sponsored or endorsed by** The Walt Disney Company, 20th Century Studios, 20th Television, Gracie Films, Matt Groening or any other rights holder of *The Simpsons*.

"The Simpsons", its characters, names, episode titles, images, audio, scripts and associated trademarks are the property of their respective owners. Nothing in this repository grants or implies any license to third-party intellectual property, and no third-party assets are distributed here.

## Planned stack and approach

Decisions approved so far:

- **Java 21** (OpenJDK)
- **Spring Boot** (planned)
- **Maven Wrapper** — the project will not require a global Maven installation; the wrapper keeps builds reproducible
- **PostgreSQL** running in **Docker Compose** for development — the database container will **not** be started until backend development actually requires it
- Incremental delivery: small, reviewable changes with verification at each step

## Principles

- Quality over speed
- No secrets, credentials, tokens or private data in the repository — ever
- Reproducible environments (lockfiles, wrapper distributions, documented steps)
- Every change verified before it lands

## Development workflow

This project follows an incremental, review-first workflow: each phase is proposed, reviewed and approved before implementation. See the commit history for the evolution of the project.

## License

This project's original code is released under the **MIT License** — see [LICENSE](LICENSE).

The MIT license applies **exclusively to the original code authored for this repository**. It does **not** grant any rights over The Simpsons — its trademarks, names, characters, episodes, images, audio, scripts — or any other third-party intellectual property. No licenses to third-party intellectual property are granted or implied by this project.
