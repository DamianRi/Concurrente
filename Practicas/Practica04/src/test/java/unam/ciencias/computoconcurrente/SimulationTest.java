package unam.ciencias.computoconcurrente;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class SimulationTest {
    static long SIMULATION_DURATION_MS = 10000;
    static long VERIFICATION_WAIT_MS = 200;
    static double ACCEPTANCE_DELTA = 0.2;

    int validExclusion;
    int invalidExclusion;

    Toilette toilette;
    List<Participant> participants;
    List<Thread> threads;

    @BeforeEach
    void setUp() {
        this.toilette = new Toilette();
        this.validExclusion = 0;
        this.invalidExclusion = 0;
    }

    @Test
    void testOneMaleAndOneFemale() throws Exception {
        runTestCase(1,1);
        verifyState();
    }

    @Test
    void testSeveralMaleAndOneFemale() throws Exception {
        runTestCase(6,1);
        verifyState();
    }

    @Test
    void testOneMaleAndSeveralFemale() throws Exception {
        runTestCase(1,8);
        verifyState();
    }

    @Test
    void testSeveralMaleAndSeveralFemale() throws Exception {
        runTestCase(5,7);
        verifyState();
    }

    void runTestCase(int males, int females) throws Exception {
        System.out.printf("\nRunning tests case males %d and females %d\n", males, females);

        init(males, females);
        startThreads();

        Thread.sleep(SIMULATION_DURATION_MS);

        interruptThreads();
        waitThreadsToFinish();
    }

    void init(int males, int females) throws Exception {
        this.participants = new ArrayList<>(males + females);
        this.threads = new ArrayList<>(males + females);

        initParticipants(males, Male.class);
        initParticipants(females, Female.class);
        initThreads();
        initExclusionVerificationThread();
    }

    void initParticipants(int participants, Class<? extends Participant> clazz) throws Exception {
        while(participants-- > 0) {
            this.participants.add(clazz.getDeclaredConstructor(Toilette.class).newInstance(toilette));
        }
    }

    void initThreads() {
        for(Participant p : this.participants) {
            this.threads.add(new Thread(p));
        }
    }

    void initExclusionVerificationThread() {
        this.threads.add(new Thread(this::verifyToiletteExclusion));
    }

    /*
     * A thread is going to analyse the exclusion property of the toilette.
     */
    void verifyToiletteExclusion() {
        try {
            while(true) {
                Thread.sleep(VERIFICATION_WAIT_MS);

                long males = this.toilette.getTimesMalesEntered(),
                        females = this.toilette.getTimesFemalesEntered();

                boolean condition = (males>=0 && females==0) || (males==0 && females>=0);
                this.validExclusion += condition ? 1 : 0;
                this.invalidExclusion += condition ? 0 : 1;
            }
        }
        catch(InterruptedException ie) {
            System.out.printf("Verification thread finishing simulation. Valid exclusion: %d. Invalid Exclusion %d\n",
                    validExclusion, invalidExclusion);
        }
    }

    void startThreads() {
        for(Thread t : this.threads) {
            t.start();
        }
    }

    void interruptThreads() {
        for(Thread t : this.threads) {
            t.interrupt();
        }
    }

    void waitThreadsToFinish() throws InterruptedException {
        for(Thread t : this.threads) {
            t.join();
        }
    }

    void verifyState() {
        long timesToiletteWasUsed = this.toilette.getTimesMalesEntered() + this.toilette.getTimesFemalesEntered();

        long timesParticipantsUsedTheToilette = participants.stream()
                .mapToLong(Participant::getTimesEnteredTheToilette)
                .sum();

        boolean allParticipantsEnteredTheToilette = participants.stream()
                .map(p -> p.getTimesEnteredTheToilette() > 0).reduce(true, (v1, v2) -> v1 && v2);

        // Consistent numbers in the simulation
        assertTrue(timesParticipantsUsedTheToilette >= timesToiletteWasUsed-threads.size()
                && timesParticipantsUsedTheToilette <= timesToiletteWasUsed);
        // All participants used the toilette
        assertTrue(allParticipantsEnteredTheToilette);

        // invalid exclusion equal to zero is difficult unless we read males and females atomically.
        assertTrue(this.invalidExclusion >= 0 && this.validExclusion > 0);
        assertTrue((1.0*this.invalidExclusion)/this.validExclusion < ACCEPTANCE_DELTA);
    }
}
