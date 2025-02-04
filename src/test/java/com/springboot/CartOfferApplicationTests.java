package com.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.controller.OfferRequest;
import com.springboot.controller.SegmentResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.mockserver.integration.ClientAndServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CartOfferApplicationTests {

	private ClientAndServer mockServer;
	private final String BASE_URL = "http://localhost:9001";

	@Before
	public void setUpServer(){
		mockServer = startClientAndServer(1080);
		setupUserSegmentMock(1,"p1");  //Mocking user1 as segment p1
	}

	//As mentioned in the assignment,need to mock api to fetch segment of a user
	private void setupUserSegmentMock(int userId, String segment) {
		mockServer.when(HttpRequest.request()
						.withMethod("GET")
						.withPath("/api/v1/user_segment")
						.withQueryStringParameter("user_id", "1"))
				.respond(HttpResponse.response()
						.withBody("{\"segment\":\"" + segment + "\"}")
						.withStatusCode(200));
	}
	public boolean addOffer(OfferRequest offerRequest) throws Exception {
		String urlString = BASE_URL+ "/api/v1/offer";
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/json");

		ObjectMapper mapper = new ObjectMapper();

		String POST_PARAMS = mapper.writeValueAsString(offerRequest);
		OutputStream os = con.getOutputStream();
		os.write(POST_PARAMS.getBytes());
		os.flush();
		os.close();
		int responseCode = con.getResponseCode();
		System.out.println("POST Response Code :: " + responseCode);

		if (responseCode == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// print result
			System.out.println(response.toString());
		} else {
			System.out.println("POST request did not work.");
		}
		return true;
	}

	//Method to apply offer
	public int applyOffer(int userId,int cartValue,int restaurantId) throws Exception{
		String urlString = BASE_URL + "/api/v1/cart/apply_offer";
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestMethod("POST");

		//Request Body
		String POST_PARAMS = "{\"cart_value\":" + cartValue + ",\"user_id\":" + userId + ",\"restaurant_id\":" + restaurantId + "}";
		OutputStream os = con.getOutputStream();
		os.write(POST_PARAMS.getBytes());
		os.flush();
		os.close();
		int responseCode = con.getResponseCode();
		System.out.println("POST Response Code :: " + responseCode);

		if (responseCode == HttpURLConnection.HTTP_OK) { //success 200 OK
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			ObjectMapper mapper = new ObjectMapper();
			//Return cart value
			return mapper.readTree(response.toString()).get("cart_value").asInt();
		}
		else {
			return -1;
		}
	}

	/*All the test Cases for Cart Offer Application
	TEST CASE 1:
	Check if offer is added successfully
	 */
	@Test
	public void checkFlatXForOneSegment() throws Exception {
		List<String> segments = new ArrayList<>();
		segments.add("p1");
		OfferRequest offerRequest = new OfferRequest(1,"FLATX",10,segments);
		boolean result = addOffer(offerRequest);
		Assert.assertEquals(result,true); // able to add offer
	}

	/*
	TEST CASE 2:
	Test to check if flat x amount of discount works properly
	*/

	@Test
	public void checkFlatXAmountOffer() throws  Exception{
		setupUserSegmentMock(1,"p1");
		List<String> segments = new ArrayList<>();
		segments.add("p1");
		OfferRequest offerRequest=new OfferRequest(1,"FLATX",20,segments);
		boolean result=addOffer(offerRequest);
		Assert.assertEquals(result,true); // able to add offer

		int finalCartValue=applyOffer(1,200,1);
		Assert .assertEquals(180,finalCartValue);
	}

	/*
	TEST CASE 3:
	Test to check if flat x percent of discount works properly
	 */
	@Test
	public void checkFlatXPercentOffer() throws Exception{
		setupUserSegmentMock(1,"p1");

		List<String> segments = new ArrayList<>();
		segments.add("p1");
		OfferRequest offerRequest=new OfferRequest(1,"FLATX%",20,segments);
		boolean result=addOffer(offerRequest);
		Assert.assertEquals(result,true); // able to add offer

		int finalCartValue=applyOffer(1,200,1);
		Assert .assertEquals(160,finalCartValue);
	}

	/*TEST CASE 4:
	Test to check multiple offers for the same segment
	 */
	@Test
	public void checkMultipleOffersSameSegment() throws Exception{
		setupUserSegmentMock(1,"p1");

		List<String> segments = new ArrayList<>();
		segments.add("p1");

		//Adding offer request by amount
		OfferRequest offerRequestbyAmount=new OfferRequest(1,"FLATX",10,segments);
		boolean result1=addOffer(offerRequestbyAmount);
		Assert.assertEquals(result1,true); // able to add offer

		//Adding offer request by percent
		OfferRequest offerRequestByDiscount=new OfferRequest(1,"FLATX%",10,segments);
		boolean result2=addOffer(offerRequestbyAmount);
		Assert.assertEquals(result1,true); // able to add offer

		int finalCartValue=applyOffer(1,200,1);
		Assert .assertEquals(170,finalCartValue);   // First 10% off (180), then -10 (170)

	}

	/*TEST CASE 5:
	Test to check INVALID OFFER TYPE
	 */
	@Test
	public void checkInvalidOfferType() throws Exception {
		setupUserSegmentMock(1, "p1");

		List<String> segments = new ArrayList<>();
		segments.add("p1");

		boolean result = addOffer(new OfferRequest(1, "INVALID_OFFER", 10,segments));
		Assert.assertFalse(result); // Offer should not be added
	}

	/*TEST CASE 6:
	Test to check Zero Discount offer
	 */
	@Test
	public void checkZeroDiscount() throws Exception {
		List<String> segments = new ArrayList<>();
		segments.add("p1");
		setupUserSegmentMock(1, "p1");

		addOffer(new OfferRequest(1, "FLATX", 0, segments));

		int finalCartValue = applyOffer(1, 200, 1);
		Assert.assertEquals(200, finalCartValue); // No discount applied
	}



	/*TEST CASE 7:
	Test to check if offer value is greater than cart value.
	 */
	@Test
	public void checkOfferGreaterThanCartValue() throws Exception {
		List<String> segments = new ArrayList<>();
		segments.add("p1");
		setupUserSegmentMock(1, "p1");

		addOffer(new OfferRequest(1, "FLATX", 300,segments));

		int finalCartValue = applyOffer(1, 200, 1);
		Assert.assertEquals(0, finalCartValue); // Cart value cannot be negative, so should be 0
	}

	/*
	Test Case 8:
	Test to check invalid segment
	*/
	@Test
	public void checkInvalidSegment()throws Exception{
		setupUserSegmentMock(1, "p1");

		List<String> segments = new ArrayList<>();
		segments.add("p4");

		boolean result = addOffer(new OfferRequest(1, "FLATX", 10,segments));
		Assert.assertFalse(result); // Offer should not be added
	}

	/*
	Test Case 9:
	Test to check applying offer before adding first
	 */
	@Test
	public void checkApplyOfferBeforeAdd()throws  Exception{
		int finalCartValue = applyOffer(1, 200, 1); // Apply offer without adding it first
		Assert.assertEquals(200, finalCartValue); // Cart value should remain unchanged
	}

	/*
	Test Case 10:
	Test to check Re-applying the same offer twice
	 */
	@Test
	public void checkReApplyingSameOfferTwice() throws Exception {
		List<String> segments = new ArrayList<>();
		segments.add("p1");
		setupUserSegmentMock(1, "p1");

		addOffer(new OfferRequest(1, "FLATX", 20, segments));

		int firstApplication = applyOffer(1, 200, 1);
		Assert.assertEquals(180, firstApplication); // First discount applied correctly

		int secondApplication = applyOffer(1, 180, 1);
		Assert.assertEquals(180, secondApplication); // Offer should not apply twice
	}


	/*
	Test case 11:
	Test to check Invalid Restaurant Id
	 */
	@Test
	public void checkInvalidRestaurantId() throws Exception {
		List<String> segments = new ArrayList<>();
		segments.add("p1");
		setupUserSegmentMock(1, "p1");

		boolean result = addOffer(new OfferRequest(999, "FLATX", 10, segments)); // Invalid restaurant ID
		Assert.assertFalse(result); // Offer should not be added
	}

	/*
	Test Case 12:
	Test case to check zero cart value
	 */
	 @Test
	public void checkZeroCartValue() throws Exception {
		List<String> segments = new ArrayList<>();
		segments.add("p1");
		setupUserSegmentMock(1, "p1");

		addOffer(new OfferRequest(1, "FLATX", 10, segments));

		int finalCartValue = applyOffer(1, 0, 1); // Cart value is zero
		Assert.assertEquals(0, finalCartValue); // Cart should remain zero
	}

}
