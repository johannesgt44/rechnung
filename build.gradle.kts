plugins {
    id("java")
    id("application")
    id("com.google.protobuf") version "0.9.4"
}

group = "com.acme"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val grpcVersion = "1.73.0"
val camundaVersion = "8.8.0"
val protobufVersion = "3.25.5"

dependencies {
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("io.camunda:camunda-client-java:$camundaVersion")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

application {
    mainClass = "com.acme.rechnung.metadata.RechnungMetadataServer"
}

tasks.register<JavaExec>("runRechnungsmetadatenWorker") {
    group = "application"
    description = "Startet den Camunda Job Worker zum Speichern von Rechnungsmetadaten."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.acme.rechnung.camunda.RechnungsmetadatenSpeichernWorker"
}

tasks.test {
    useJUnitPlatform()
}
