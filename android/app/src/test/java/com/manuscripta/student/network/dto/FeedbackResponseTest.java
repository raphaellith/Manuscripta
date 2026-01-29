package com.manuscripta.student.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link FeedbackResponse}.
 * Tests the response wrapper for the GET /feedback/{deviceId} endpoint.
 *
 * <p>Per API Contract §2.6, the response format wraps a list of feedback items.</p>
 */
public class FeedbackResponseTest {

    private static final String TEST_FEEDBACK_ID_1 = "fb-11111111-1111-1111-1111-111111111111";
    private static final String TEST_FEEDBACK_ID_2 = "fb-22222222-2222-2222-2222-222222222222";
    private static final String TEST_RESPONSE_ID_1 = "resp-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String TEST_RESPONSE_ID_2 = "resp-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Test
    public void testDefaultConstructor() {
        FeedbackResponse response = new FeedbackResponse();

        assertNull(response.getFeedback());
    }

    @Test
    public void testConstructorWithFeedbackList() {
        List<FeedbackDto> feedbackList = createTestFeedbackList();

        FeedbackResponse response = new FeedbackResponse(feedbackList);

        assertNotNull(response.getFeedback());
        assertEquals(2, response.getFeedback().size());
        assertEquals(feedbackList, response.getFeedback());
    }

    @Test
    public void testConstructorWithNullList() {
        FeedbackResponse response = new FeedbackResponse(null);

        assertNull(response.getFeedback());
    }

    @Test
    public void testConstructorWithEmptyList() {
        FeedbackResponse response = new FeedbackResponse(Collections.emptyList());

        assertNotNull(response.getFeedback());
        assertTrue(response.getFeedback().isEmpty());
    }

    @Test
    public void testSetFeedback() {
        FeedbackResponse response = new FeedbackResponse();
        List<FeedbackDto> feedbackList = createTestFeedbackList();

        response.setFeedback(feedbackList);

        assertNotNull(response.getFeedback());
        assertEquals(2, response.getFeedback().size());
    }

    @Test
    public void testSetFeedbackToNull() {
        FeedbackResponse response = new FeedbackResponse(createTestFeedbackList());

        response.setFeedback(null);

        assertNull(response.getFeedback());
    }

    @Test
    public void testSetFeedbackToEmptyList() {
        FeedbackResponse response = new FeedbackResponse(createTestFeedbackList());

        response.setFeedback(new ArrayList<>());

        assertNotNull(response.getFeedback());
        assertTrue(response.getFeedback().isEmpty());
    }

    @Test
    public void testGetFeedbackReturnsCorrectItems() {
        FeedbackDto feedback1 = new FeedbackDto(TEST_FEEDBACK_ID_1, TEST_RESPONSE_ID_1,
                "Great work!", 90);
        FeedbackDto feedback2 = new FeedbackDto(TEST_FEEDBACK_ID_2, TEST_RESPONSE_ID_2,
                "Needs improvement", 45);

        FeedbackResponse response = new FeedbackResponse(Arrays.asList(feedback1, feedback2));

        List<FeedbackDto> result = response.getFeedback();
        assertEquals(2, result.size());
        assertEquals(TEST_FEEDBACK_ID_1, result.get(0).getId());
        assertEquals(TEST_FEEDBACK_ID_2, result.get(1).getId());
        assertEquals("Great work!", result.get(0).getText());
        assertEquals(Integer.valueOf(90), result.get(0).getMarks());
    }

    @Test
    public void testSingleFeedbackItem() {
        FeedbackDto feedback = new FeedbackDto(TEST_FEEDBACK_ID_1, TEST_RESPONSE_ID_1,
                "Excellent!", 100);

        FeedbackResponse response = new FeedbackResponse(Collections.singletonList(feedback));

        assertNotNull(response.getFeedback());
        assertEquals(1, response.getFeedback().size());
        assertEquals(TEST_FEEDBACK_ID_1, response.getFeedback().get(0).getId());
    }

    @Test
    public void testFeedbackWithTextOnly() {
        FeedbackDto feedback = new FeedbackDto(TEST_FEEDBACK_ID_1, TEST_RESPONSE_ID_1,
                "Good effort but review section 3", null);

        FeedbackResponse response = new FeedbackResponse(Collections.singletonList(feedback));

        FeedbackDto result = response.getFeedback().get(0);
        assertEquals("Good effort but review section 3", result.getText());
        assertNull(result.getMarks());
    }

    @Test
    public void testFeedbackWithMarksOnly() {
        FeedbackDto feedback = new FeedbackDto(TEST_FEEDBACK_ID_1, TEST_RESPONSE_ID_1,
                null, 75);

        FeedbackResponse response = new FeedbackResponse(Collections.singletonList(feedback));

        FeedbackDto result = response.getFeedback().get(0);
        assertNull(result.getText());
        assertEquals(Integer.valueOf(75), result.getMarks());
    }

    @Test
    public void testFeedbackPreservesOrder() {
        FeedbackDto feedback1 = new FeedbackDto("id-1", "resp-1", "First", 10);
        FeedbackDto feedback2 = new FeedbackDto("id-2", "resp-2", "Second", 20);
        FeedbackDto feedback3 = new FeedbackDto("id-3", "resp-3", "Third", 30);

        List<FeedbackDto> orderedList = Arrays.asList(feedback1, feedback2, feedback3);
        FeedbackResponse response = new FeedbackResponse(orderedList);

        List<FeedbackDto> result = response.getFeedback();
        assertEquals("id-1", result.get(0).getId());
        assertEquals("id-2", result.get(1).getId());
        assertEquals("id-3", result.get(2).getId());
    }

    @Test
    public void testMixedFeedbackTypes() {
        // Some feedback with text, some with marks, some with both
        FeedbackDto textOnly = new FeedbackDto("id-text", "resp-1", "Comment only", null);
        FeedbackDto marksOnly = new FeedbackDto("id-marks", "resp-2", null, 50);
        FeedbackDto both = new FeedbackDto("id-both", "resp-3", "With marks", 80);

        FeedbackResponse response = new FeedbackResponse(
                Arrays.asList(textOnly, marksOnly, both));

        List<FeedbackDto> result = response.getFeedback();
        assertEquals(3, result.size());

        // Text only
        assertNotNull(result.get(0).getText());
        assertNull(result.get(0).getMarks());

        // Marks only
        assertNull(result.get(1).getText());
        assertNotNull(result.get(1).getMarks());

        // Both
        assertNotNull(result.get(2).getText());
        assertNotNull(result.get(2).getMarks());
    }

    @Test
    public void testLargeFeedbackList() {
        List<FeedbackDto> largeFeedbackList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeFeedbackList.add(new FeedbackDto(
                    "id-" + i,
                    "resp-" + i,
                    "Feedback " + i,
                    i
            ));
        }

        FeedbackResponse response = new FeedbackResponse(largeFeedbackList);

        assertEquals(100, response.getFeedback().size());
        assertEquals("id-0", response.getFeedback().get(0).getId());
        assertEquals("id-99", response.getFeedback().get(99).getId());
    }

    private List<FeedbackDto> createTestFeedbackList() {
        FeedbackDto feedback1 = new FeedbackDto(
                TEST_FEEDBACK_ID_1,
                TEST_RESPONSE_ID_1,
                "Well done!",
                85
        );
        FeedbackDto feedback2 = new FeedbackDto(
                TEST_FEEDBACK_ID_2,
                TEST_RESPONSE_ID_2,
                "Consider revising paragraph 2",
                70
        );
        return Arrays.asList(feedback1, feedback2);
    }
}
