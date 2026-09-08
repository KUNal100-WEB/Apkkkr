package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ExamType
import com.example.data.model.MistakeCategory
import com.example.data.model.PracticeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Adaptive Exam Tutor", appName)
  }

  @Test
  fun `verify competitive exam targets and practice modes`() {
    val exams = ExamType.values()
    assertTrue("Target exams should not be empty", exams.isNotEmpty())
    assertTrue("Should include SSC CGL", exams.any { it == ExamType.SSC_CGL })

    val modes = PracticeMode.values()
    assertTrue("Practice modes should be defined", modes.isNotEmpty())
  }

  @Test
  fun `verify 13 mistake categories are defined`() {
    val categories = MistakeCategory.values()
    assertEquals(13, categories.size)
    assertNotNull(MistakeCategory.CONCEPTUAL)
    assertNotNull(MistakeCategory.SIGN)
    assertNotNull(MistakeCategory.TIME_PRESSURE)
  }

}

