package dev.jsekhon;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import com.twilio.Twilio;
import com.twilio.type.PhoneNumber;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class NotificationLambda implements RequestHandler<Object, String> {

    private static final String DYNAMODB_TABLE_NAME = System.getenv("USER_DB_AWS");
    private static final String EMAIL_NOTIFICATION_COLUMN = System.getenv("emailNotiColumn");
    private static final String PHONE_NOTIFICATION_COLUMN = System.getenv("phoneNotiColumn");
    private static final String AWS_SENDER_EMAIL = System.getenv("AWS_SENDER_EMAIL");
    private static final String Account_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String Auth_Token = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String twilioPhoneNumber = System.getenv("TWILIO_PHN_NUM");

    public String handleRequest(Object input, Context context) {

        Map<String, Object> inputMap = (Map<String, Object>) input;

        String category = (String) inputMap.get("category");


        System.out.println("Category: " + category);

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

                addEmailRecipient(item, emailRecipients, category);
                addPhoneRecipient(item, phoneRecipients, category);

            }

            sendEmailNotifications(emailRecipients);
            sendSMSNotifications(phoneRecipients);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        // Print the contents of emailRecipients and phoneRecipients for testing
        printRecipients(emailRecipients, "Email Recipients:");
        printRecipients(phoneRecipients, "Phone Recipients:");

        return "success";

    }

    private void sendEmailNotifications(List<String> recipients) {
        String SUBJECT = "New Blog Posted!";
        String HTML_BODY = "You can find the blog <a href=\"https://master.d3rmuxe1kj9gm3.amplifyapp.com/\">here</a>.</p>";
        String TEXT_BODY = "You can find the blog at: https://master.d3rmuxe1kj9gm3.amplifyapp.com/";


        // Create an SES client
        SesClient client = SesClient.builder()
                .region(Region.US_WEST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        // Create the email request
        SendEmailRequest request = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(recipients).build())
                .message(Message.builder()
                        .body(Body.builder()
                                .html(Content.builder().data(HTML_BODY).build())
                                .text(Content.builder().data(TEXT_BODY).build())
                                .build())
                        .subject(Content.builder().data(SUBJECT).build())
                        .build())
                .source(AWS_SENDER_EMAIL)
                .build();

        // Send the email
        try {
            client.sendEmail(request);
            System.out.println("Email sent successfully.");
        } catch (SesException e) {
            System.out.println("Email sending failed: " + e.getMessage());
        }
    }

    private void sendSMSNotifications(List<String> recipients) {
        Twilio.init(Account_SID, Auth_Token);

        for (String recipient : recipients) {
            com.twilio.rest.api.v2010.account.Message message = com.twilio.rest.api.v2010.account.Message.creator(
                    new PhoneNumber(recipient),
                    new PhoneNumber(twilioPhoneNumber),
                    "A New blog has been posted! You can find the blog at: https://master.d3rmuxe1kj9gm3.amplifyapp.com/")
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