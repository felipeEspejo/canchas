plugins {
    java
    application
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.felipe"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "dev.felipe.canchas.CanchasApp"
}

repositories {
    mavenCentral()
}

dependencies {
    // adaptador de vistas: Spring + Thymeleaf + login
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

    // adaptador REST
    implementation("io.javalin:javalin:7.2.4")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")


    // persistencia: JDBI + H2
    implementation("org.jdbi:jdbi3-core:3.55.0")      //jdbi
    implementation("org.jdbi:jdbi3-sqlobject:3.55.0") //jdbi
    runtimeOnly("com.microsoft.sqlserver:mssql-jdbc")  // driver de Microsoft SQL Server

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2") // las pruebas de repositorios corren en H2 en modo SQL Server (sin instalar nada)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8" // evita textos con tildes dañados al compilar en Windows
}

tasks.test {
    useJUnitPlatform()
}
