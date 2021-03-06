package org.axonframework.test;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.junit.*;

import java.util.concurrent.Executor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
public class FixtureTest_Resources {

    private FixtureConfiguration<AggregateWithResources> fixture;

    @Before
    public void setUp() {
        fixture = Fixtures.newGivenWhenThenFixture(AggregateWithResources.class);
    }

    @Test
    public void testResourcesAreScopedToSingleTest_ConstructorPartOne() {
        // executing the same test should pass, as resources are scoped to a single test only
        final Executor resource = mock(Executor.class);
        fixture.registerInjectableResource(resource)
               .given()
               .when(new CreateAggregateCommand("id"));

        verify(resource).execute(isA(Runnable.class));
        verifyNoMoreInteractions(resource);
    }

    @Test
    public void testResourcesAreScopedToSingleTest_ConstructorPartTwo() {
        testResourcesAreScopedToSingleTest_ConstructorPartOne();
    }

    @Test
    public void testResourcesAreScopedToSingleTest_MethodPartOne() {
        // executing the same test should pass, as resources are scoped to a single test only
        final Executor resource = mock(Executor.class);
        fixture.registerInjectableResource(resource)
               .given(new MyEvent("id", 1))
               .when(new TestCommand("id"))
               .expectReturnValue(fixture.getCommandBus());

        verify(resource).execute(isA(Runnable.class));
        verifyNoMoreInteractions(resource);
    }

    @Test
    public void testResourcesAreScopedToSingleTest_MethodPartTwo() {
        testResourcesAreScopedToSingleTest_MethodPartOne();
    }

    public static class AggregateWithResources extends AbstractAnnotatedAggregateRoot<String> {

        @AggregateIdentifier
        private String id;

        @CommandHandler
        public AggregateWithResources(CreateAggregateCommand cmd, Executor resource, CommandBus commandBus) {
            apply(new MyEvent(cmd.getAggregateIdentifier(), 1));
            resource.execute(new Runnable() {
                @Override
                public void run() {
                    fail("Should not really be executed");
                }
            });
        }

        @CommandHandler
        public CommandBus handleCommand(TestCommand cmd, Executor resource, CommandBus commandBus) {
            resource.execute(new Runnable() {
                @Override
                public void run() {
                    fail("Should not really be executed");
                }
            });
            return commandBus;
        }

        @EventHandler
        void handle(MyEvent event) {
            this.id = event.getAggregateIdentifier().toString();
        }

        public AggregateWithResources() {
        }
    }
}
