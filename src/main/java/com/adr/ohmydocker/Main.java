package com.adr.ohmydocker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;


public class Main {

    public static void main(String[] args) throws InterruptedException {

        // Create default docker client
        DockerClient docker = DockerClientBuilder.getInstance().build();
        
        String result = invokeFunction(docker, "Oh my Docker");

        System.out.println(result);
    }

    private static String invokeFunction(DockerClient docker, String input) {

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

    private static String invokeFunctionWithExitCode(DockerClient docker, String input) {

        // Create and execute the container
        CreateContainerResponse container = docker
                .createContainerCmd("busybox")
                .withCmd("/bin/echo", "Hello from " + input + "!")
                .exec();
        docker.startContainerCmd(container.getId()).exec();

        // Wait for the execution of the command
        int exitCode = docker.waitContainerCmd(container.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode();
        if (exitCode != 0) {
            throw new RuntimeException("Error running container.");
        }

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
