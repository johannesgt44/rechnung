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

val grpcVersion = "1.65.1"
val jacksonVersion = "2.17.2"
val protobufVersion = "3.25.5"
val rabbitmqVersion = "5.22.0"

dependencies {
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.rabbitmq:amqp-client:$rabbitmqVersion")
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

tasks.register<JavaExec>("runZahlungService") {
    group = "application"
    description = "Startet den RabbitMQ-Zahlungsdienst."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.acme.rechnung.zahlung.ZahlungsServiceWorker"
}

tasks.register<JavaExec>("runRechnungsClient") {
    group = "application"
    description = "Startet den Rechnungs-Client."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.acme.rechnung.client.RechnungClient"
}

tasks.test {
    useJUnitPlatform()
}
