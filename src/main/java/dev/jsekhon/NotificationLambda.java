package dev.jsekhon;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class NotificationLambda implements RequestHandler<Object, String> {

    private static final String DYNAMODB_TABLE_NAME = System.getenv("USER_DB_AWS");
    private static final String EMAIL_NOTIFICATION_COLUMN = System.getenv("emailNotiColumn");
    private static final String PHONE_NOTIFICATION_COLUMN = System.getenv("phoneNotiColumn");

    private static final String Account_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String Auth_Token = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String twilioPhoneNumber = System.getenv("TWILIO_PHN_NUM");

    public String handleRequest(Object input, Context context) {

//        AttributeValue submittedGenre = tech;

        DynamoDbClient client = DynamoDbClient.builder()
                .region(Region.US_WEST_2)
                .build();

        List<String> emailRecipients = new ArrayList<>();
        List<String> phoneRecipients = new ArrayList<>();

        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(DYNAMODB_TABLE_NAME)
                    .build();

            ScanResponse response = client.scan(scanRequest);
            for (Map<String, AttributeValue> item : response.items()) {

                addEmailRecipient(item, emailRecipients, "tech");
                addPhoneRecipient(item, phoneRecipients, "tech");

                sendEmailNotifications(emailRecipients);
                sendSMSNotifications(phoneRecipients);

            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        // Print the contents of emailRecipients and phoneRecipients for testing
        printRecipients(emailRecipients, "Email Recipients:");
        printRecipients(phoneRecipients, "Phone Recipients:");

        return "success";

    }

    private void sendEmailNotifications(List<String> recipients) {
//        String FROM = "blogrblogs@gmail.com";
//        String SUBJECT = "New Blog Posted!";
//        String HTML_BODY = "<p>Your email body goes here.</p>";
//        String TEXT_BODY = "Your email body goes here.";
//
//        // Create an SES client
//        SesClient client = SesClient.builder()
//                .region(Region.US_WEST_1)
//                .credentialsProvider(DefaultCredentialsProvider.create())
//                .build();
//
//        // Create the email request
//        SendEmailRequest request = SendEmailRequest.builder()
//                .destination(Destination.builder().toAddresses(recipients).build())
//                .message(Message.builder()
//                        .body(Body.builder()
//                                .html(Content.builder().data(HTML_BODY).build())
//                                .text(Content.builder().data(TEXT_BODY).build())
//                                .build())
//                        .subject(Content.builder().data(SUBJECT).build())
//                        .build())
//                .source(FROM)
//                .build();
//
//        // Send the email
//        try {
//            client.sendEmail(request);
//            System.out.println("Email sent successfully.");
//        } catch (SesException e) {
//            System.out.println("Email sending failed: " + e.getMessage());
//        }
    }

    private void sendSMSNotifications(List<String> recipients) {
        Twilio.init(Account_SID, Auth_Token);

        for (String recipient : recipients) {
            Message message = Message.creator(
                    new PhoneNumber(recipient),
                    new PhoneNumber(twilioPhoneNumber),
                    "Hello from Twilio!")
                    .create();
        }

    }

    private void printRecipients(List<String> recipients, String title) {
        System.out.println(title);
        for (String recipient : recipients) {
            System.out.println(recipient);
        }
        System.out.println("------");
    }

    // Method to check if the genres list contains a specific genre
    private boolean containsGenre(List<AttributeValue> genresAttribute, String genre) {
        for (AttributeValue genreValue : genresAttribute) {
            if (genre.equals(genreValue.s())) {
                return true;
            }
        }
        return false;
    }

    // Method to add email recipient if the genres list contains a specific genre
    private void addEmailRecipient(Map<String, AttributeValue> item, List<String> emailRecipients, String genre) {
        List<AttributeValue> genresAttribute = item.get("genres").l();
        if (containsGenre(genresAttribute, genre) && item.get(EMAIL_NOTIFICATION_COLUMN).bool()) {
            String email = item.get("email").s();
            if (EmailValidator.isValid(email)) {
                emailRecipients.add(email);
            }
        }
    }

    private void addPhoneRecipient(Map<String, AttributeValue> item, List<String> phoneRecipients, String genre) {
        List<AttributeValue> genresAttribute = item.get("genres").l();
        if (containsGenre(genresAttribute, genre) && item.get(PHONE_NOTIFICATION_COLUMN).bool()) {
            String phoneNumber = item.get("phoneNumber").s();
            if (PhoneNumberValidator.isValid(phoneNumber)) {
                phoneRecipients.add(phoneNumber);
            }
        }
    }
}