package dev.jsekhon;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class NotificationLambda implements RequestHandler<Object, String> {

    private static final String DYNAMODB_TABLE_NAME = "USER_SUB_TABLE_NAME";
    private static final String EMAIL_NOTIFICATION_COLUMN = "sendEmailNoti";
    private static final String PHONE_NOTIFICATION_COLUMN = "sendPhoneNoti";

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
        // Code to send email notifications via Amazon SES
    }

    private void sendSMSNotifications(List<String> recipients) {
        // Code to send SMS notifications (implement this part)
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