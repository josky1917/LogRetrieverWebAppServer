package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Sch utility class with methods for SSH command execution.
 */
public final class Ssh {
    private static final Logger LOGGER = LogManager.getLogger(Region.class);

    private static final int POLL_INTERVAL_MILLIS = 50;

    private static final long COMMAND_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(2);

    private static final int CONNECT_TIMEOUT_MILLIS = (int) TimeUnit.SECONDS.toMillis(30);

    private Ssh() {
    }

    /**
     * Connect to a given host using a given identity, retrieving the session handle.
     *
     * @param host     The Non-null host to connect to
     * @param userName username
     * @param provider Non-null AWS credential provider
     * @return A Non-null connected session which must be disconnected when it is no longer needed
     * @throws IOException on error
     */
    public static Session connect(final String host, String userName, AWSCredentialsProvider provider)
            throws IOException {
        checkNotNull(host);
        checkNotNull(provider);
        LOGGER.info("Establishing ssh connection to " + host);
        Stopwatch stopwatch = Stopwatch.createStarted();
        final JSch jsch = new JSch();
        String username = provider.getCredentials().getAWSAccessKeyId();
        LOGGER.info("Got username " + username + " --should be same as ?--- " + userName);
        byte[] privateKey = provider.getCredentials().getAWSSecretKey().getBytes(StandardCharsets.UTF_8);
        LOGGER.info("Got Key.");
        try {
            jsch.addIdentity(username, privateKey, null, null);
            Session session = jsch.getSession(username, host);
            Properties config = new Properties();
            config.setProperty("StrictHostKeyChecking", "no");
            session.setConfig(config);
            LOGGER.info("Start connecting. Timeout millis is " + CONNECT_TIMEOUT_MILLIS);
            session.connect(CONNECT_TIMEOUT_MILLIS);
            LOGGER.info("Established ssh connection to " + host + " in "
                    + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " millis");
            return session;
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    /**
     * Use SSH to execute a given command.
     *
     * @param session The Non-null session to connect with
     * @param command The Non-null command to run
     * @return A Non-null SshCmdResult containing the stdout and return code
     * @throws IOException on error
     */
    public static SshCmdResult sshCmd(final Session session, final String command) throws IOException {
        checkNotNull(session);
        checkNotNull(command);
        LOGGER.info("Running ssh command \"" + command);
        Stopwatch stopwatch = Stopwatch.createStarted();
        ChannelExec channel = null;
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try {
            // open a channel to execute commands
            channel = (ChannelExec) session.openChannel("exec");
            channel.setPty(true);
            channel.setCommand(command);
            channel.setInputStream(null);

            // set the output streams
            channel.setErrStream(stderr);
            channel.setOutputStream(stdout);

            // execute the command
            channel.connect();

            // wait for it to finish
            long counter = 0;
            while (!channel.isEOF() && counter * POLL_INTERVAL_MILLIS < COMMAND_TIMEOUT_MILLIS) {
                counter++;
                Uninterruptibles.sleepUninterruptibly(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
                if (counter % 100 == 0) {
                    LOGGER.info("Polled " + counter + " times for command " + command
                            + ", \n| channel.isClosed(): " + channel.isClosed()
                            + ", \n| session.isConnected(): " + session.isConnected()
                            + ", \n| channel.isConnected(): " + channel.isConnected() + "\n");
                }
            }

            LOGGER.info("Poll completed in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " millis");

            if (!channel.isEOF()) {
                channel.sendSignal("KILL");
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
        return new SshCmdResult(channel.getExitStatus(),
                new String(stdout.toByteArray(), StandardCharsets.UTF_8),
                new String(stderr.toByteArray(), StandardCharsets.UTF_8));
    }

    /**
     * Simple wrapper for the command stdout and status code.
     */
    public static class SshCmdResult {

        private final int fStatusCode;
        private final String fStdout;
        private final String fStderr;

        /**
         * @param statusCode The Non-null status code returned by the command
         * @param stdout     The Non-null stdout returned by the command
         * @param stderr     The Non-null stderr returned by the command
         */
        public SshCmdResult(final int statusCode, final String stdout, final String stderr) {
            checkNotNull(stdout);
            checkNotNull(stderr);
            fStatusCode = statusCode;
            fStdout = stdout;
            fStderr = stderr;
        }

        /**
         * @return The status code returned by the command
         */
        public int getStatusCode() {
            return fStatusCode;
        }

        /**
         * @return The stdout returned by the command
         */
        public String getStdout() {
            return fStdout;
        }

        /**
         * @return The stderr returned by the command
         */
        public String getStderr() {
            return fStderr;
        }
    }

}
