rootProject.name = "backend-interview-labs"

include(
    ":labs:concurrency",
    ":labs:collections",
    ":labs:jvm",
    ":labs:async"
)

// Import standalone exercise as a composite build for IDE source discovery.
// Student's unfinished solution is not part of root build/test tasks.
includeBuild("exercises/concurrency/semaphore-limited-service")
