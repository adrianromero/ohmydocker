package com.adr.ohmydocker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import spark.Spark;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        // Create default docker client
        DockerClient docker = DockerClientBuilder.getInstance().build();

        // http://localhost:4567/ohmydocker/:name
        Spark.get("/ohmydocker/:name", (request, response) -> String.format(
                "Result: %s\n",
                serverlessFunction(docker, request.params(":name"))));
    }

    private static String serverlessFunction(DockerClient docker, String input) {

        // Create and execute the container
        CreateContainerResponse container = docker
                .createContainerCmd("busybox")
                .withCmd("/bin/echo", "Hello from " + input + "!")
                .exec();
        docker.startContainerCmd(container.getId()).exec();

        // Get the result
        LogContainerPayloadCallback loggingCallback = new LogContainerPayloadCallback();
        docker.logContainerCmd(container.getId())
                .withStdErr(true)
                .withStdOut(true)
                .exec(loggingCallback);

        try {
            // And wait for the result
            loggingCallback.awaitCompletion();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Error running container.", ex);
        }

        // Remove the container
        docker.removeContainerCmd(container.getId()).exec();

        return loggingCallback.toString();
    }

    private static class LogContainerPayloadCallback extends LogContainerResultCallback {

        protected final StringBuffer log = new StringBuffer();

        @Override
        public void onNext(Frame frame) {
            log.append(new String(frame.getPayload()));
        }

        @Override
        public String toString() {
            return log.toString();
        }
    }
}
