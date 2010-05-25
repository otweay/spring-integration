/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.junit.rules.ExpectedException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Oleg Zhurakousky
 * @author Dave Syer
 */
public class MethodInvokingMessageProcessorTests {

	private static final Log logger = LogFactory.getLog(MethodInvokingMessageProcessorTests.class);
	
	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void testHandlerInheritanceMethodImplInSuper() {
		class A {
			@SuppressWarnings("unused")
			public Message<String> myMethod(final Message<String> msg) {
		        return MessageBuilder.fromMessage(msg).setHeader("A", "A").build();
		    }
		}

		class B extends A {}

		@SuppressWarnings("unused")
		class C extends B {}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new B(), "myMethod");
		Message<?> message = (Message<?>) processor.processMessage(new StringMessage(""));
		assertEquals("A", message.getHeaders().get("A"));
	}

	@Test
	public void testHandlerInheritanceMethodImplInLatestSuper() {
		class A {
			@SuppressWarnings("unused")
			public Message<String> myMethod(Message<String> msg) {
		        return MessageBuilder.fromMessage(msg).setHeader("A", "A").build();
		    }
		}

		class B extends A {
			public Message<String> myMethod(Message<String> msg) {
		        return MessageBuilder.fromMessage(msg).setHeader("B", "B").build();
		    }
		}

		@SuppressWarnings("unused")
		class C extends B {}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new B(), "myMethod");
		Message<?> message = (Message<?>) processor.processMessage(new StringMessage(""));
		assertEquals("B", message.getHeaders().get("B"));
	}

	public void testHandlerInheritanceMethodImplInSubClass() {
		class A {
			@SuppressWarnings("unused")
			public Message<String> myMethod(Message<String> msg) {
		        return MessageBuilder.fromMessage(msg).setHeader("A", "A").build();
		    }
		}

		class B extends A {
			public Message<String> myMethod(Message<String> msg) {
		        return MessageBuilder.fromMessage(msg).setHeader("B", "B").build();
		    }
		}

		class C extends B {
			public Message<String> myMethod(Message<String> msg) {
		        return MessageBuilder.fromMessage(msg).setHeader("C", "C").build();
		    }
		}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new C(), "myMethod");
		Message<?> message = (Message<?>) processor.processMessage(new StringMessage(""));
		assertEquals("C", message.getHeaders().get("C"));
	}

	public void testHandlerInheritanceMethodImplInSubClassAndSuper() {
		class A {
			@SuppressWarnings("unused")
			public Message<String> myMethod(Message<String> msg){
		        return MessageBuilder.fromMessage(msg).setHeader("A", "A").build();
		    }
		}

		class B extends A {}

		class C extends B {
			public Message<String> myMethod(Message<String> msg) {
		        return MessageBuilder.fromMessage(msg).setHeader("C", "C").build();
		    }
		}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new C(), "myMethod");
		Message<?> message = (Message<?>) processor.processMessage(new StringMessage(""));
		assertEquals("C", message.getHeaders().get("C"));
	}

	@Test
	public void payloadAsMethodParameterAndObjectAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(
				new TestBean(), "acceptPayloadAndReturnObject");
		Object result = processor.processMessage(new StringMessage("testing"));
		assertEquals("testing-1", result);
	}

	@Test
	public void payloadAsMethodParameterAndMessageAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(
				new TestBean(), "acceptPayloadAndReturnMessage");
		Message<?> result = (Message<?>) processor.processMessage(new StringMessage("testing"));
		assertEquals("testing-2", result.getPayload());
	}

	@Test
	public void messageAsMethodParameterAndObjectAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(
				new TestBean(), "acceptMessageAndReturnObject");
		Object result = processor.processMessage(new StringMessage("testing"));
		assertEquals("testing-3", result);
	}

	@Test
	public void messageAsMethodParameterAndMessageAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(
				new TestBean(), "acceptMessageAndReturnMessage");
		Message<?> result = (Message<?>) processor.processMessage(new StringMessage("testing"));
		assertEquals("testing-4", result.getPayload());
	}

	@Test
	public void messageSubclassAsMethodParameterAndMessageAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(
				new TestBean(), "acceptMessageSubclassAndReturnMessage");
		Message<?> result = (Message<?>) processor.processMessage(new StringMessage("testing"));
		assertEquals("testing-5", result.getPayload());
	}

	@Test
	public void messageSubclassAsMethodParameterAndMessageSubclassAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(
				new TestBean(), "acceptMessageSubclassAndReturnMessageSubclass");
		Message<?> result = (Message<?>) processor.processMessage(new StringMessage("testing"));
		assertEquals("testing-6", result.getPayload());
	}

	@Test
	public void payloadAndHeaderAnnotationMethodParametersAndObjectAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(
				new TestBean(), "acceptPayloadAndHeaderAndReturnObject");
		Message<?> request = MessageBuilder.withPayload("testing")
				.setHeader("number", new Integer(123)).build();
		Object result = processor.processMessage(request);
		assertEquals("testing-123", result);
	}

    @Test
    public void testVoidMethodsIncludedbyDefault() {
        MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(), "testVoidReturningMethods");
        assertNull(processor.processMessage(MessageBuilder.withPayload("Something").build()));
        assertEquals(12, processor.processMessage(MessageBuilder.withPayload(12).build()));
    }

    @Test
    public void testVoidMethodsExcludedByFlag() {
    	Exception exception = null;
        MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(), "testVoidReturningMethods", true);
        assertEquals(12, processor.processMessage(MessageBuilder.withPayload(12).build()));
        try {
        	processor.processMessage(MessageBuilder.withPayload("not_a_number").build());
            fail();
        }
        catch(MessageHandlingException ex) {
        	// the only void method expects a number
        	exception = ex;
        }
        assertNotNull(exception);
    }

	@Test
	public void messageOnlyWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("messageOnly", Message.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Object result = processor.processMessage(new StringMessage("foo"));
		assertEquals("foo", result);
	}

	@Test
	public void payloadWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Object result = processor.processMessage(new GenericMessage<Integer>(new Integer(123)));
		assertEquals(new Integer(123), result);
	}

	@Test
	public void convertedPayloadWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Object result = processor.processMessage(new StringMessage("456"));
		assertEquals(new Integer(456), result);
	}

	@Test(expected = MessageHandlingException.class)
	public void conversionFailureWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		processor.processMessage(new StringMessage("foo"));
	}

	@Test
	public void testProcessMessageBadExpression() throws Exception {
		expected.expect(new ExceptionCauseMatcher(ConversionFailedException.class));
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		assertEquals("foo", processor.processMessage(new StringMessage("foo")));
	}

	@Test
	public void testProcessMessageRuntimeException() throws Exception {
		expected.expect(new ExceptionCauseMatcher(UnsupportedOperationException.class));
		TestErrorService service = new TestErrorService();
		Method method = service.getClass().getMethod("error", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		assertEquals("foo", processor.processMessage(new StringMessage("foo")));
	}

	@Test
	public void testProcessMessageCheckedException() throws Exception {
		expected.expect(new ExceptionCauseMatcher(CheckedException.class));
		TestErrorService service = new TestErrorService();
		Method method = service.getClass().getMethod("checked", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		assertEquals("foo", processor.processMessage(new StringMessage("foo")));
	}

	@Test
	public void messageAndHeaderWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("messageAndHeader", Message.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("number", 42).build();
		Object result = processor.processMessage(message);
		assertEquals("foo-42", result);
	}

	@Test
	public void multipleHeadersWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("twoHeaders", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("prop", "bar")
				.setHeader("number", 42).build();
		Object result = processor.processMessage(message);
		assertEquals("bar-42", result);
	}

	@Test
	public void filterSelectsAnnotationMethodsOnly() {
		AmbiguousMethodBean bean = new AmbiguousMethodBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(bean, ServiceActivator.class);
		processor.processMessage(MessageBuilder.withPayload(123).build());
		assertNotNull(bean.lastArg);
		assertEquals(String.class, bean.lastArg.getClass());
		assertEquals("123", bean.lastArg);
	}

	@Test
	public void filterSelectsNonVoidReturningMethodsOnly() {
		AmbiguousMethodBean bean = new AmbiguousMethodBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(bean, "foo", true);
		processor.processMessage(MessageBuilder.withPayload(true).build());
		assertNotNull(bean.lastArg);
		assertEquals(String.class, bean.lastArg.getClass());
		assertEquals("true", bean.lastArg);
	}
	
	
	private static class ExceptionCauseMatcher extends TypeSafeMatcher<Exception> {
		private Throwable cause;
		private Class<? extends Exception> type;
		public ExceptionCauseMatcher(Class<? extends Exception> type) {
			this.type = type;
		}
		@Override
		public boolean matchesSafely(Exception item) {
			logger.debug(item);
			cause = item.getCause();
			assertNotNull("There is no cause for "+item, cause);
			return type.isAssignableFrom(cause.getClass());
		}
		public void describeTo(Description description) {
			description.appendText("cause to be ").appendValue(type).appendText("but was ").appendValue(cause);
		}
	}

	@SuppressWarnings("unused")
	private static class TestErrorService {
		public String error(String input) {
			throw new UnsupportedOperationException("Expected test exception");
		}
		public String checked(String input) throws Exception {
			throw new CheckedException("Expected test exception");
		}
	}
	
	public static final class CheckedException extends Exception {
		public CheckedException(String string) {
			super(string);
		}
	}

	@SuppressWarnings("unused")
	private static class TestBean {

		public String acceptPayloadAndReturnObject(String s) {
			return s + "-1";
		}

		public Message<?> acceptPayloadAndReturnMessage(String s) {
			return new StringMessage(s + "-2");
		}

		public String acceptMessageAndReturnObject(Message<?> m) {
			return m.getPayload() + "-3";
		}

		public Message<?> acceptMessageAndReturnMessage(Message<?> m) {
			return new StringMessage(m.getPayload() + "-4");
		}

		public Message<?> acceptMessageSubclassAndReturnMessage(StringMessage m) {
			return new StringMessage(m.getPayload() + "-5");
		}

		public StringMessage acceptMessageSubclassAndReturnMessageSubclass(StringMessage m) {
			return new StringMessage(m.getPayload() + "-6");
		}

		public String acceptPayloadAndHeaderAndReturnObject(String s, @Header("number") Integer n) {
			return s + "-" + n;
		}

        public void testVoidReturningMethods(String s) {
            // do nothing
        }

        public int testVoidReturningMethods(int i) {
            return i;
        }

	}


	@SuppressWarnings("unused")
	private static class AnnotatedTestService {

		public String messageOnly(Message<?> message) {
			return (String) message.getPayload();
		}

		public String messageAndHeader(Message<?> message, @Header("number") Integer num) {
			return (String) message.getPayload() + "-" + num.toString();
		}

		public String twoHeaders(@Header String prop, @Header("number") Integer num) {
			return prop + "-" + num.toString();
		}

		public Integer optionalHeader(@Header(required=false) Integer num) {
			return num;
		}

		public Integer requiredHeader(@Header(value="num", required=true) Integer num) {
			return num;
		}

		public String optionalAndRequiredHeader(@Header(required=false) String prop, @Header(value="num", required=true) Integer num) {
			return prop + num;
		}

		public Properties propertiesMethod(Properties properties) {
			return properties;
		}

		@SuppressWarnings("unchecked")
		public Map mapMethod(Map map) {
			return map;
		}

		public Integer integerMethod(Integer i) {
			return i;
		}

	}


	/**
	 * Method names create ambiguities, but the MethodResolver implementation
	 * should filter out based on the annotation or the 'requiresReply' flag.
	 */
	@SuppressWarnings("unused")
	private static class AmbiguousMethodBean {

		private volatile Object lastArg = null;

		public void foo(boolean b) {
			this.lastArg = b;
		}

		@ServiceActivator
		public String foo(String s) {
			this.lastArg = s;
			return s;
		}

		public String foo(int i) {
			this.lastArg = i;
			return Integer.valueOf(i).toString();
		}

	}

}