/*
 * Copyright (c) 2010-2012. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventhandling.annotation;

import org.axonframework.common.annotation.UnsupportedHandlerException;
import org.axonframework.domain.EventMessage;
import org.axonframework.domain.GenericEventMessage;
import org.axonframework.domain.StubDomainEvent;
import org.joda.time.DateTime;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Allard Buijze
 */
@SuppressWarnings({"UnusedDeclaration"})
public class AnnotationEventHandlerInvokerTest {

    private AnnotationEventHandlerInvoker testSubject;

    /*
    Test scenario:
    even though the super class handler is more specific, the generic one of the subclass takes precedence
    */

    @Test
    public void testInvokeEventHandler_SubClassHasPriority() {
        SecondSubclass secondSubclass = new SecondSubclass();
        testSubject = new AnnotationEventHandlerInvoker(secondSubclass);
        testSubject.invokeEventHandlerMethod(new GenericEventMessage<StubEventTwo>(new StubEventTwo()));

        assertEquals("Method handler 1 shouldn't be invoked. Calls", 0, secondSubclass.invocationCount1);
        assertEquals("Method handler 2 shouldn't be invoked. Calls", 0, secondSubclass.invocationCount2);
        assertEquals("Expected Method handler 3 to be invoked. Calls", 1, secondSubclass.invocationCount3);
    }

    @Test
    public void testInvokeEventHandler_SuperClassTakesPrecedenceOverInterface() {
        ListeningToInterface handler = new ListeningToInterface();
        testSubject = new AnnotationEventHandlerInvoker(handler);
        testSubject.invokeEventHandlerMethod(new GenericEventMessage<StubEventTwo>(new StubEventTwo()));

        assertEquals("Handler was not triggered. Do interfaces get priority over implementations?", 1, handler.invocationCount2);
        assertEquals("The interface seemed to get priority over a superclass", 0, handler.invocationCount1);
    }

    @Test
    public void testInvokeEventHandler_MatchesAgainstInterface() {
        ListeningToInterface handler = new ListeningToInterface();
        testSubject = new AnnotationEventHandlerInvoker(handler);
        testSubject.invokeEventHandlerMethod(new GenericEventMessage<SomeInterface>(mock(SomeInterface.class)));

        assertEquals("Wrong handler triggered", 0, handler.invocationCount2);
        assertEquals("Expected match based on implemented interface", 1, handler.invocationCount1);
    }

    /*
    Test scenario:
    within a single class, the most specific handler is chosen, even if an exact handler isn't found.
    */

    @Test
    public void testInvokeEventHandler_MostSpecificHandlerInClassChosen() {
        FirstSubclass handler = new FirstSubclass();
        testSubject = new AnnotationEventHandlerInvoker(handler);
        testSubject
                .invokeEventHandlerMethod(new GenericEventMessage<StubEventTwo>(new StubEventTwo() {/*anonymous subclass*/
                }));

        assertEquals(0, handler.invocationCount1);
        assertEquals(1, handler.invocationCount2);
    }

    @Test
    public void testInvokeEventHandler_UnknownEventIsIgnored() {
        FirstSubclass handler = new FirstSubclass();
        testSubject = new AnnotationEventHandlerInvoker(handler);
        testSubject
                .invokeEventHandlerMethod(new GenericEventMessage<StubDomainEvent>(new StubDomainEvent() {/*anonymous subclass*/
                }));

        assertEquals(0, handler.invocationCount1);
        assertEquals(0, handler.invocationCount2);
    }

    /*
    Test scenario:
    within a single class, the most specific handler is chosen, even if an exact handler isn't found.
    */

    @Test
    public void testValidateEventHandler_PrimitiveFirstParameterIsRejected() {
        FirstSubclass handler = new IllegalEventHandler();
        try {
            new AnnotationEventHandlerInvoker(handler);
            fail("Expected an UnsupportedHandlerException");
        } catch (UnsupportedHandlerException e) {
            assertTrue(e.getMessage().contains("notARealHandler"));
            assertEquals("notARealHandler", e.getViolatingMethod().getName());
        }
    }

    @Test
    public void testValidateEventHandler_WrongSecondsParameterIsRejected() {
        FirstSubclass handler = new ASecondIllegalEventHandler();
        try {
            new AnnotationEventHandlerInvoker(handler);
            fail("Expected an UnsupportedHandlerException");
        } catch (UnsupportedHandlerException e) {
            assertTrue(e.getMessage().contains("notARealHandler"));
            assertEquals("notARealHandler", e.getViolatingMethod().getName());
        }
    }

    /*
    Test scenario:
    a method called handle with single parameter of type DomainEvent is not allowed. It conflicts with the proxy.
     */

    @Test
    public void testValidateEventHandler_HandleDomainEventIsRejected() {
        FirstSubclass handler = new EventHandlerWithUnfortunateMethod();
        try {
            new AnnotationEventHandlerInvoker(handler);
            fail("Expected an UnsupportedHandlerException");
        } catch (UnsupportedHandlerException e) {
            assertTrue(e.getMessage().contains("conflict"));
            assertEquals("handle", e.getViolatingMethod().getName());
        }
    }

    @Test
    public void testValidateEventHandler_DuplicateHandler() {
        DuplicateHandlerMethod handler = new DuplicateHandlerMethod();
        try {
            new AnnotationEventHandlerInvoker(handler);
            fail("Expected an UnsupportedHandlerException");
        } catch (UnsupportedHandlerException e) {
            assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("otherHandler"));
            assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("oneHandler"));
            assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("DuplicateHandlerMethod"));
        }
    }

    private static class FirstSubclass {

        protected int invocationCount1;
        protected int invocationCount2;

        /*
        return values are allowed, but ignored
         */

        @EventHandler
        public boolean method1(StubEventOne event) {
            invocationCount1++;
            return true;
        }

        @EventHandler
        public void method2(StubEventTwo event) {
            invocationCount2++;
        }

    }

    private static class ListeningToInterface {

        protected int invocationCount1;
        protected int invocationCount2;

        @EventHandler
        public void handle(SomeInterface event) {
            invocationCount1++;
        }

        @EventHandler(eventType = StubEventOne.class)
        public void handle() {
            invocationCount2++;
        }
    }

    private static class SecondSubclass extends FirstSubclass {

        protected int invocationCount3;

        @EventHandler(eventType = StubEventOne.class)
        public void method3(@Timestamp DateTime dateTime) {
            invocationCount3++;
        }
    }

    private static class IllegalEventHandler extends SecondSubclass {

        @EventHandler
        public void notARealHandler(int event, String thisParameterMakesItIncompatible) {
        }
    }

    private static class ASecondIllegalEventHandler extends SecondSubclass {

        @EventHandler
        public void notARealHandler(StubEventTwo event, String thisParameterMakesItIncompatible) {
        }
    }

    private static class DuplicateHandlerMethod {

        @EventHandler
        public void oneHandler(StubEventOne event) {
        }

        @EventHandler(eventType = StubEventOne.class)
        public void otherHandler() {
        }
    }

    private static class StubEventOne extends StubDomainEvent {

    }

    private static class StubEventTwo extends StubEventOne implements SomeInterface {

    }

    private static interface SomeInterface {

    }

    private class EventHandlerWithUnfortunateMethod extends FirstSubclass {

        @EventHandler
        public void handle(EventMessage event) {
        }
    }
}
