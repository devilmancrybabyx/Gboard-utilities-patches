extension {
    name = "extensions/gboard-calculator.rve"
}

android {
    namespace = "dev.jason.gboardpatches.calculator"
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
