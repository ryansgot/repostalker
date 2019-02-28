package com.fsryan.repostalker.nav

import com.fsryan.repostalker.data.nav.Navigator

import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

class NavigatorTest {

    private lateinit var navigatorUnderTest: Navigator
    private lateinit var testObserver: TestObserver<String>

    @BeforeEach
    fun setUpNavigator() {
        navigatorUnderTest = Navigator.create(Schedulers.trampoline())
        testObserver = TestObserver()
        navigatorUnderTest.backEventObservable().subscribe(testObserver)
    }

    @AfterEach
    fun flushTestObserver() {
        testObserver.dispose()
    }

    @Test
    @DisplayName("should throw when attempting to pop from empty stack")
    fun popEmptyStack() {
        Assertions.assertThrows(Exception::class.java) {
            navigatorUnderTest.pop().blockingGet()
        }
    }

    @Test
    @DisplayName("should throw when attempting to peek empty stack")
    fun peekEmptyStack() {
        Assertions.assertThrows(Exception::class.java) {
            navigatorUnderTest.peek().blockingGet()
        }
    }

    @Test
    @DisplayName("Should return top of stack when peeking")
    fun peek() {
        val expected = "second"
        navigatorUnderTest.push("first").blockingGet()
        navigatorUnderTest.push(expected).blockingGet()
        val actual = navigatorUnderTest.peek().blockingGet()
        assertEquals("1|$expected", actual)
    }

    @Test
    @DisplayName("should pop in reverse order of of push")
    fun poppingOrder() {
        val ids = listOf("bottom", "bottom-middle", "middle", "top-middle", "top")
        for (id in ids) {
            navigatorUnderTest.push(id).blockingGet()
        }

        for (i in 0 until ids.size) {   // <-- popped in reverse order of push
            navigatorUnderTest.pop().blockingGet()
        }

        testObserver.assertValues("4|top", "3|top-middle", "2|middle", "1|bottom-middle", "0|bottom")
    }
}