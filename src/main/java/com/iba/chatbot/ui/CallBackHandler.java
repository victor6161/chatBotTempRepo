package com.iba.chatbot.ui;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.common.SupportedLocale;
import com.github.messenger4j.common.WebviewHeightRatio;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.messengerprofile.MessengerSettings;
import com.github.messenger4j.messengerprofile.getstarted.StartButton;
import com.github.messenger4j.messengerprofile.greeting.Greeting;
import com.github.messenger4j.messengerprofile.greeting.LocalizedGreeting;
import com.github.messenger4j.messengerprofile.persistentmenu.LocalizedPersistentMenu;
import com.github.messenger4j.messengerprofile.persistentmenu.PersistentMenu;
import com.github.messenger4j.messengerprofile.persistentmenu.action.PostbackCallToAction;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.SenderActionPayload;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.LocationQuickReply;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.ListTemplate;
import com.github.messenger4j.send.message.template.ReceiptTemplate;
import com.github.messenger4j.send.message.template.button.*;
import com.github.messenger4j.send.message.template.common.Element;
import com.github.messenger4j.send.message.template.receipt.Address;
import com.github.messenger4j.send.message.template.receipt.Adjustment;
import com.github.messenger4j.send.message.template.receipt.Item;
import com.github.messenger4j.send.message.template.receipt.Summary;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.send.senderaction.SenderAction;
import com.github.messenger4j.userprofile.UserProfile;
import com.github.messenger4j.webhook.Event;
import com.github.messenger4j.webhook.event.*;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.attachment.LocationAttachment;
import com.github.messenger4j.webhook.event.attachment.RichMediaAttachment;
import com.iba.chatbot.ui.fsm.*;
import com.iba.chatbot.ui.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;

import static com.github.messenger4j.Messenger.*;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@RestController
@RequestMapping("/callback")
public class CallBackHandler {


    private static final String RESOURCE_URL = "https://raw.githubusercontent.com/fbsamples/messenger-platform-samples/master/node/public";

    private static final Logger logger = LoggerFactory.getLogger(CallBackHandler.class);

    private final Messenger messenger;

    private Map<String, UserSession> userSessionMaps = new WeakHashMap<>();

    private List<Condition> conditions = Arrays.asList(new Condition("MENU"),new Condition("FIRST_WORKING_DAY"), new Condition("REVIEW_USELESS"),
            new Condition("REVIEW_USELESS"), new Condition("REVIEW_ABUSE"));


    @Autowired
    public CallBackHandler(final Messenger messenger) {
        this.messenger = messenger;
    }

    /**
     * Webhook verification endpoint. <p> The passed verification token (as query parameter) must match the configured
     * verification token. In case this is true, the passed challenge string must be returned by this endpoint.
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> verifyWebhook(@RequestParam(MODE_REQUEST_PARAM_NAME) final String mode,
                                                @RequestParam(VERIFY_TOKEN_REQUEST_PARAM_NAME) final String verifyToken, @RequestParam(CHALLENGE_REQUEST_PARAM_NAME) final String challenge) {
        logger.debug("Received Webhook verification request - mode: {} | verifyToken: {} | challenge: {}", mode, verifyToken, challenge);
        try {
            this.messenger.verifyWebhook(mode, verifyToken);
            return ResponseEntity.ok(challenge);
        } catch (MessengerVerificationException e) {
            logger.warn("Webhook verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
    private void initMenu() throws MessengerApiException, MessengerIOException {
        final PostbackCallToAction callToMenu =
                PostbackCallToAction.create(
                        "Menu", "menu_item");
        final PostbackCallToAction callToActionHelp =
                PostbackCallToAction.create(
                        "Help", "ACTION_HELP");
        final PersistentMenu persistentMenu =
                PersistentMenu.create(
                        false,
                        of(Arrays.asList(callToMenu, callToActionHelp)),
                        LocalizedPersistentMenu.create(SupportedLocale.ru_RU, false, empty()));

        final Greeting greeting =
                Greeting.create(
                        "Вас приветствует бот помощник!",
                        LocalizedGreeting.create(SupportedLocale.ru_RU, "Timeless apparel for the masses."));

        final MessengerSettings messengerSettings1 =
                MessengerSettings.create(
                        java.util.Optional.of(StartButton.create("Button pressed")), of(greeting), of(persistentMenu), empty(), empty(), empty(), empty());
        this.messenger.updateSettings(messengerSettings1);

    }
    /**
     * Callback endpoint responsible for processing the inbound messages and events.
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> handleCallback(@RequestBody final String payload, @RequestHeader(SIGNATURE_HEADER_NAME) final String signature) {
        logger.debug("Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
        try
        {
            this.messenger.onReceiveEvents(payload, of(signature), event -> {
              /*  if (userSessionMaps.get(event.senderId()) != null && userSessionMaps.get(event.senderId()).isMenuUpdateNeeded()) {
                    try {
                        initMenu();
                    } catch (MessengerApiException e) {
                        logger.warn("Processing of callback payload failed: {}", e.getMessage());
                    } catch (MessengerIOException e) {
                        logger.warn("Processing of callback payload failed: {}", e.getMessage());
                    }
                }*/
                if (event.isTextMessageEvent()) {
                    final String messageId = event.asTextMessageEvent().messageId();
                    final String messageText = event.asTextMessageEvent().text();
                    final String senderId = event.asTextMessageEvent().senderId();
                    //final Instant timestamp = event.asTextMessageEvent().timestamp().toString();
                    handleTextMessageEvent(messageId, messageText, senderId, null);

                } else if( event.isPostbackEvent()) {
                    logger.debug("Handling PostbackEvent");
                    final String messageText = event.asPostbackEvent().payload().orElse("empty payload");
                    logger.debug("messageText: {}", messageText);
                    final String senderId = event.senderId();
                    logger.debug("senderId: {}", senderId);

                    handleTextMessageEvent(null, messageText, senderId, null);
                } else if (event.isAttachmentMessageEvent()) {
                    handleAttachmentMessageEvent(event.asAttachmentMessageEvent());
                } else if (event.isQuickReplyMessageEvent()) {
                    handleQuickReplyMessageEvent(event.asQuickReplyMessageEvent());
                }
               /* else if (event.isPostbackEvent()) {
                    handlePostbackEvent(event.asPostbackEvent());
                } */ else if (event.isAccountLinkingEvent()) {
                    handleAccountLinkingEvent(event.asAccountLinkingEvent());
                } else if (event.isOptInEvent()) {
                    handleOptInEvent(event.asOptInEvent());
                } else if (event.isMessageEchoEvent()) {
                    handleMessageEchoEvent(event.asMessageEchoEvent());
                } else if (event.isMessageDeliveredEvent()) {
                    handleMessageDeliveredEvent(event.asMessageDeliveredEvent());
                } else if (event.isMessageReadEvent()) {
                    handleMessageReadEvent(event.asMessageReadEvent());
                } else {
                    handleFallbackEvent(event);
                }

            });
            logger.debug("Processed callback payload successfully");
            return ResponseEntity.status(HttpStatus.OK).build();
        }catch (MessengerVerificationException e) {
            logger.warn("Processing of callback payload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } /*catch (MessengerIOException e) {

        } catch (MessengerApiException e) {
            logger.warn("Processing of callb" +
                    "ack payload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }*/ /*catch (MalformedURLException e) {
            logger.warn("Processing of callb" +
                    "ack payload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }*/
    }

    private void handleTextMessageEvent(String messageId, String messageText, String senderId, String timestamp) {
        logger.debug("Received TextMessageEvent: {}", messageText);

 /*       final String messageId = event.messageId();
        final String messageText = event.text();
        final String senderId = event.senderId();
        final Instant timestamp = event.timestamp();*/
        UserSession userSession = userSessionMaps.get(senderId);

        if (userSession != null) {
            userSession = userSessionMaps.get(senderId);
            if (TypeNextActionEnum.COMMAND == userSession.getStateMachine().getCurrent().getTypeNextActionEnum()) {
               Condition conditionsFromUser = null;

                for (Condition condition : conditions) {
                    if(condition.getCondition().equals(messageText)) {
                        conditionsFromUser = condition;
                        break;
                    }
                }
                if (conditionsFromUser != null) {
                    userSession.getStateMachine().apply(conditionsFromUser);
                }
                sendTextMessage(senderId,  userSession.getStateMachine().getCurrent().getText());
            } else {
                // TEXT handling
            }
        } else {
            setUpUserSession(senderId);
            sendUserMenu(senderId);
        }




     /*   logger.info("Received message '{}' with text '{}' from user '{}' at '{}'", messageId, messageText, senderId, timestamp);
        try {*/


                    /*
                case "user":
                    sendUserDetails(senderId);
                    break;
                case "image":
                    sendImageMessage(senderId);
                    break;

                case "gif":
                    sendGifMessage(senderId);
                    break;

                case "audio":
                    sendAudioMessage(senderId);
                    break;

                case "video":
                    sendVideoMessage(senderId);
                    break;

                case "file":
                    sendFileMessage(senderId);
                    break;

                case "button":
                    sendButtonMessage(senderId);
                    break;

                case "generic":
                    sendGenericMessage(senderId);
                    break;

                case "list":
                    sendListMessageMessage(senderId);
                    break;

                case "receipt":
                    sendReceiptMessage(senderId);
                    break;

                case "quick reply":
                    sendQuickReply(senderId);
                    break;

                case "read receipt":
                    sendReadReceipt(senderId);
                    break;

                case "typing on":
                    sendTypingOn(senderId);
                    break;

                case "typing off":
                    sendTypingOff(senderId);
                    break;

                case "account linking":
                    sendAccountLinking(senderId);
                    break;
                */
               /* default:
                    sendTextMessage(senderId, messageText);*/

   /*         } else if (TypeStepEnum.MENU.equals(userSessionMaps.get(senderId).getCurrentState().getTypeStepEnum())) {
                switch (messageText) {
                    case "First working day":
                        UserSession userSession = userSessionMaps.get(senderId);
                        //userSession.setSteps(FlowStorage.FIRST_DAY_FLOWS);
                        userSession.setCurrentState(FlowStorage.FIRST_DAY_FLOWS.get(1));
                        sendTextMessage(senderId, FlowStorage.FIRST_DAY_FLOWS.get(0).getMessage());
                        break;*/
                   /* case "Health care":
                        InfoFlow healthCareFlow = new InfoFlow();
                        List<TypeStepEnum> healthCareFlowTypeEnums = new ArrayList<>();
                        healthCareFlowTypeEnums.add(TypeStepEnum.INFO);
                        healthCareFlowTypeEnums.add(TypeStepEnum.REVIEW);
                        healthCareFlow.setFlowTypeEnums(healthCareFlowTypeEnums);
                        sendTextMessageWithType(senderId, "list of hospitals, medical center, insurance", "health_care_type", "review");
                        break;*/
                   /* case "Sport":
                        sendTextMessageWithType(senderId, "list of building and season ticket", "sport_type", "review");
                        break;
                    case "Facilities":
                        sendTextMessageWithType(senderId, "who is responsible for changing tables, chairs and so on", "facilites_type", "review");
                        break;*/
                /*    case "Application":
                        InfoFlow applicationFlow = new InfoFlow();
                        List<TypeStepEnum> applicationFlowTypeEnums = new ArrayList<>();
                        applicationFlowTypeEnums.add(TypeStepEnum.INFO);
                        applicationFlowTypeEnums.add(TypeStepEnum.REVIEW);
                        applicationFlow.setFlowTypeEnums(applicationFlowTypeEnums);
                        sendTextMessageWithType(senderId, "Please, Write down a organization for you application ", "application_type", "application_term");
                        break;*/
        /*            case "Instructions":
                        //sendTextMessageWithType(senderId, "What instructions do you need ? examples 'Wi-fi'", "instructions_type", "review");
                        break;
                }
            } else {

                if(TypeStepEnum.REVIEW.equals(userSessionMaps.get(senderId).getCurrentState().getTypeStepEnum())) {
                    userSessionMaps.get(senderId).setCurrentState(FlowStorage.FIRST_DAY_FLOWS.get(2));
                    createReviewButtons(senderId);
                } else if (TypeStepEnum.REVIEW_RECEIVE.equals(userSessionMaps.get(senderId).getCurrentState().getTypeStepEnum())) {
                    if("useless".equals(messageText)) {
                        userSessionMaps.get(senderId).setCurrentState(new MessageBotEntity(TypeStepEnum.ABUSE,""));
                        sendTextMessage(senderId, FlowStorage.FIRST_DAY_FLOWS.get(0).getMessage());
                    } else {
                        userSessionMaps.get(senderId).setCurrentState(FlowStorage.FIRST_DAY_FLOWS.get(2));
                        sendTextMessage(senderId, FlowStorage.FIRST_DAY_FLOWS.get(0).getMessage());
                    }
                }
            }*/


        /*    else if("application_term".equals(userSessionMaps.get(senderId).getStep())) {
                userSessionMaps.get(senderId).setStep("application_organization");
                logger.debug("Received TextMessageEvent: {}", messageText);
                sendTextMessage(senderId, "please, type a number of month to me");
            } else if("application_organization".equals(userSessionMaps.get(senderId).getStep())) {
                userSessionMaps.get(senderId).setStep("application_organization_end");
                logger.debug("Received TextMessageEvent: {}", messageText);
                sendTextMessage(senderId, "What organizations need to specify?");
            } else if("application_organization_end".equals(userSessionMaps.get(senderId).getStep())) {
                userSessionMaps.get(senderId).setStep("menu_item");//
                logger.debug("Received TextMessageEvent: {}", messageText);
                sendTextMessage(senderId, "thanks a lot we've received it.");
            } else if("review".equals(userSessionMaps.get(senderId).getStep())) {
                switch (messageText) {
                    case "useful":
                        userSessionMaps.get(senderId).setStep("menu_item");
                        logger.info("" );
                        sendEndMessage(senderId, "great thanks a lot");
                        break;
                    case "useless":
                        userSessionMaps.get(senderId).setStep("review_abuse");
                        sendTextMessage(senderId, "please, type a message to me");
                        break;
                }
            }  else if ("review_abuse".equals(userSessionMaps.get(senderId).getStep())) {
                logger.info("" + messageText);
                userSessionMaps.get(senderId).setStep("menu_item");
                sendAbuseMessage(senderId, "Your abuse is received.");
            }
*/


    /*    } catch (MessengerApiException | MessengerIOException *//*| MalformedURLException *//*e) {
            handleSendException(e);
        }*/
    }

    private void setUpUserSession(String senderId) {
        UserProfile userProfile = null;
        try {
            userProfile = this.messenger.queryUserProfile(senderId);
        } catch (MessengerApiException e) {
            logger.error("error getting user data", e.getMessage());
        } catch (MessengerIOException e) {
            logger.error("error getting user data", e.getMessage());
        }
        StateMachine stateMachine = createStateMachine();
        userSessionMaps.put(senderId, new UserSession(true, userProfile, stateMachine));
    }

    private StateMachine createStateMachine() {
        State menu = new State("MENU", "It's menu of flow chatbot", TypeNextActionEnum.COMMAND);
        State firstWorkingDay = new State("FIRST_WORKING_DAY", "It's text for first working day", TypeNextActionEnum.COMMAND);
        State reviewUseless = new State("REVIEW_USELESS", "Please, type messages to me", TypeNextActionEnum.TEXT);
        State reviewAbuse = new State("REVIEW_ABUSE", "Your abuse is received.", TypeNextActionEnum.COMMAND);
        State reviewUseful = new State("REVIEW_USEFUL", "Great! Thanks a lot!", TypeNextActionEnum.COMMAND);

        Condition firstWorkingDayCondition = new Condition("FIRST_WORKING_DAY");
        Condition uselessCondition = new Condition("REVIEW_USELESS");
        Condition abuseCondition = new Condition("REVIEW_ABUSE");
        Condition usefulCondition = new Condition("REVIEW_USEFUL");

        Set<Condition> firstWorkingDayConditions = new HashSet<>();
        firstWorkingDayConditions.add(firstWorkingDayCondition);

        Set<Condition> uselessConditions = new HashSet<>();
        uselessConditions.add(uselessCondition);

        Set<Condition> usefulConditions = new HashSet<>();
        usefulConditions.add(usefulCondition);

        Set<Condition> abuseConditions = new HashSet<>();
        abuseConditions.add(abuseCondition);

        List<Transition> transitions = new ArrayList<>();
        transitions.add(new Transition(menu, firstWorkingDayConditions, firstWorkingDay));
        transitions.add(new Transition(firstWorkingDay, uselessConditions, reviewUseless));
        transitions.add(new Transition(firstWorkingDay, usefulConditions, reviewUseful));
        transitions.add(new Transition(reviewUseless, abuseConditions, reviewAbuse));

        StateMachine stateMachine = new StateMachine(menu, transitions);
        return stateMachine;
    }

    private void createReviewButtons(String senderId) throws MessengerApiException, MessengerIOException {
        final PostbackButton buttonB1 = PostbackButton.create("useful", "useful");
        final PostbackButton buttonB2 = PostbackButton.create("useless", "useless");

        final List<Button> buttons = Arrays.asList(buttonB1, buttonB2);
        final ButtonTemplate buttonTemplate =
                ButtonTemplate.create("Is this tip usefull or useless?", buttons);

        final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
        final MessagePayload payload1 =
                MessagePayload.create(senderId, MessagingType.RESPONSE, templateMessage);
        messenger.send(payload1);
    }

    private void sendUserMenu(String recipientId) {
        sendTextMessage(recipientId, "It's menu of flow chatbot. Please enter what help do you need:\n " +
                "'First working day'\n " +
                "'Health care'\n " +
              /*  "'Sport'\n " +
                "'Facilities'\n" +*/
                "'Application'\n +" +
                "'Instructions'\n");
        logger.info("Chat bot menu has sent");
    }

    private void sendUserDetails(String recipientId) throws MessengerApiException, MessengerIOException {
        final UserProfile userProfile = this.messenger.queryUserProfile(recipientId);
        sendTextMessage(recipientId, String.format("Your name is %s and you are %s", userProfile.firstName(), userProfile.gender()));
        logger.info("User Profile Picture: {}", userProfile.profilePicture());
    }

    private void sendImageMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(IMAGE, new URL(RESOURCE_URL + "/assets/rift.png"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendRichMediaMessage(String recipientId, UrlRichMediaAsset richMediaAsset) throws MessengerApiException, MessengerIOException {
        final RichMediaMessage richMediaMessage = RichMediaMessage.create(richMediaAsset);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, richMediaMessage);
        this.messenger.send(messagePayload);
    }

    private void sendGifMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(IMAGE, new URL("https://media.giphy.com/media/11sBLVxNs7v6WA/giphy.gif"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendAudioMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(AUDIO, new URL(RESOURCE_URL + "/assets/sample.mp3"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendVideoMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(VIDEO, new URL(RESOURCE_URL + "/assets/allofus480.mov"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendFileMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(FILE, new URL(RESOURCE_URL + "/assets/test.txt"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendButtonMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final List<Button> buttons = Arrays.asList(
                UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/rift/"), of(WebviewHeightRatio.COMPACT), of(false), empty(), empty()),
                PostbackButton.create("Trigger Postback", "DEVELOPER_DEFINED_PAYLOAD"), CallButton.create("Call Phone Number", "+16505551234")
        );

        final ButtonTemplate buttonTemplate = ButtonTemplate.create("Tap a button", buttons);
        final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private void sendGenericMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        List<Button> riftButtons = new ArrayList<>();
        riftButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/rift/")));
        riftButtons.add(PostbackButton.create("Call Postback", "Payload for first bubble"));

        List<Button> touchButtons = new ArrayList<>();
        touchButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/touch/")));
        touchButtons.add(PostbackButton.create("Call Postback", "Payload for second bubble"));

        final List<Element> elements = new ArrayList<>();

        elements.add(
                Element.create("rift", of("Next-generation virtual reality"), of(new URL("https://www.oculus.com/en-us/rift/")), empty(), of(riftButtons)));
        elements.add(Element.create("touch", of("Your Hands, Now in VR"), of(new URL("https://www.oculus.com/en-us/touch/")), empty(), of(touchButtons)));

        final GenericTemplate genericTemplate = GenericTemplate.create(elements);
        final TemplateMessage templateMessage = TemplateMessage.create(genericTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private void sendListMessageMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        List<Button> riftButtons = new ArrayList<>();
        riftButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/rift/")));

        List<Button> touchButtons = new ArrayList<>();
        touchButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/touch/")));

        final List<Element> elements = new ArrayList<>();

        elements.add(
                Element.create("rift", of("Next-generation virtual reality"), of(new URL("https://www.oculus.com/en-us/rift/")), empty(), of(riftButtons)));
        elements.add(Element.create("touch", of("Your Hands, Now in VR"), of(new URL("https://www.oculus.com/en-us/touch/")), empty(), of(touchButtons)));

        final ListTemplate listTemplate = ListTemplate.create(elements);
        final TemplateMessage templateMessage = TemplateMessage.create(listTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private void sendReceiptMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final String uniqueReceiptId = "order-" + Math.floor(Math.random() * 1000);

        final List<Item> items = new ArrayList<>();

        items.add(Item.create("Oculus Rift", 599.00f, of("Includes: headset, sensor, remote"), of(1), of("USD"),
                of(new URL(RESOURCE_URL + "/assets/riftsq.png"))));
        items.add(Item.create("Samsung Gear VR", 99.99f, of("Frost White"), of(1), of("USD"), of(new URL(RESOURCE_URL + "/assets/gearvrsq.png"))));

        final ReceiptTemplate receiptTemplate = ReceiptTemplate
                .create("Peter Chang", uniqueReceiptId, "Visa 1234", "USD", Summary.create(626.66f, of(698.99f), of(57.67f), of(20.00f)),
                        of(Address.create("1 Hacker Way", "Menlo Park", "94025", "CA", "US")), of(items),
                        of(Arrays.asList(Adjustment.create("New Customer Discount", -50f), Adjustment.create("$100 Off Coupon", -100f))),
                        of("The Boring Company"), of(new URL("https://www.boringcompany.com/")), of(true), of(Instant.ofEpochMilli(1428444852L)));

        final TemplateMessage templateMessage = TemplateMessage.create(receiptTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private void sendQuickReply(String recipientId) throws MessengerApiException, MessengerIOException {
        List<QuickReply> quickReplies = new ArrayList<>();

        quickReplies.add(TextQuickReply.create("Action", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_ACTION"));
        quickReplies.add(TextQuickReply.create("Comedy", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_COMEDY"));
        quickReplies.add(TextQuickReply.create("Drama", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_DRAMA"));
        quickReplies.add(LocationQuickReply.create());

        TextMessage message = TextMessage.create("What's your favorite movie genre?", of(quickReplies), empty());
        messenger.send(MessagePayload.create(recipientId, MessagingType.RESPONSE, message));
    }

    private void sendReadReceipt(String recipientId) throws MessengerApiException, MessengerIOException {
        this.messenger.send(SenderActionPayload.create(recipientId, SenderAction.MARK_SEEN));
    }

    private void sendTypingOn(String recipientId) throws MessengerApiException, MessengerIOException {
        this.messenger.send(SenderActionPayload.create(recipientId, SenderAction.TYPING_ON));
    }

    private void sendTypingOff(String recipientId) throws MessengerApiException, MessengerIOException {
        this.messenger.send(SenderActionPayload.create(recipientId, SenderAction.TYPING_OFF));
    }

    private void sendAccountLinking(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        // Mandatory https
        final LogInButton buttonIn = LogInButton.create(new URL("https://<YOUR_REST_CALLBACK_URL>"));
        final LogOutButton buttonOut = LogOutButton.create();

        final List<Button> buttons = Arrays.asList(buttonIn, buttonOut);
        final ButtonTemplate buttonTemplate = ButtonTemplate.create("Log in to see an account linking callback", buttons);

        final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private void handleAttachmentMessageEvent(AttachmentMessageEvent event) {
        logger.debug("Handling QuickReplyMessageEvent");
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);
        for (Attachment attachment : event.attachments()) {
            if (attachment.isRichMediaAttachment()) {
                final RichMediaAttachment richMediaAttachment = attachment.asRichMediaAttachment();
                final RichMediaAttachment.Type type = richMediaAttachment.type();
                final URL url = richMediaAttachment.url();
                logger.debug("Received rich media attachment of type '{}' with url: {}", type, url);
                final String text = String.format("Media %s received (url: %s)", type.name(), url);
                sendTextMessage(senderId, text);
            } else if (attachment.isLocationAttachment()) {
                final LocationAttachment locationAttachment = attachment.asLocationAttachment();
                final double longitude = locationAttachment.longitude();
                final double latitude = locationAttachment.latitude();
                logger.debug("Received location information (long: {}, lat: {})", longitude, latitude);
                final String text = String.format("Location received (long: %s, lat: %s)", String.valueOf(longitude), String.valueOf(latitude));
                sendTextMessage(senderId, text);
            }
        }
    }

    private void handleQuickReplyMessageEvent(QuickReplyMessageEvent event) {
        logger.debug("Handling QuickReplyMessageEvent");
        final String payload = event.payload();
        logger.debug("payload: {}", payload);
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);
        final String messageId = event.messageId();
        logger.debug("messageId: {}", messageId);
        logger.info("Received quick reply for message '{}' with payload '{}'", messageId, payload);
        sendTextMessage(senderId, "Quick reply tapped");
    }

    private void handlePostbackEvent(PostbackEvent event) {
        logger.debug("Handling PostbackEvent");
        final String payload = event.payload().orElse("empty payload");
        logger.debug("payload: {}", payload);
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);
        final Instant timestamp = event.timestamp();
        logger.debug("timestamp: {}", timestamp);
        logger.info("Received postback for user '{}' and page '{}' with payload '{}' at '{}'", senderId, senderId, payload, timestamp);





     /*   final PostbackButton buttonB11 = PostbackButton.create("Start Chatting", "USER_DEFINED_PAYLOAD");
        final PostbackButton buttonB21 = PostbackButton.create("Start Chatting", "USER_DEFINED_PAYLOAD");
        final PostbackButton buttonB31 = PostbackButton.create("Start Chatting", "USER_DEFINED_PAYLOAD");
        *//*   final PostbackButton buttonB4 = PostbackButton.create("Start Chatting", "USER_DEFINED_PAYLOAD");*//*

        final List<Button> buttons1 = Arrays.asList(buttonB1, buttonB2, buttonB3);
        final ButtonTemplate buttonTemplate1 =
                ButtonTemplate.create("What do you want to do next?", buttons);

        final TemplateMessage templateMessage1 = TemplateMessage.create(buttonTemplate);
        final MessagePayload payload11 =
                MessagePayload.create(senderId, MessagingType.RESPONSE, templateMessage);

        try {
            messenger.send(payload11);
        } catch (MessengerApiException e) {
            e.printStackTrace();
        } catch (MessengerIOException e) {
            e.printStackTrace();
        }*/
       // sendTextMessage(senderId, "Postback event tapped");
    }

    private void handleAccountLinkingEvent(AccountLinkingEvent event) {
        logger.debug("Handling AccountLinkingEvent");
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);
        final AccountLinkingEvent.Status accountLinkingStatus = event.status();
        logger.debug("accountLinkingStatus: {}", accountLinkingStatus);
        final String authorizationCode = event.authorizationCode().orElse("Empty authorization code!!!"); //You can throw an Exception
        logger.debug("authorizationCode: {}", authorizationCode);
        logger.info("Received account linking event for user '{}' with status '{}' and auth code '{}'", senderId, accountLinkingStatus, authorizationCode);
        sendTextMessage(senderId, "AccountLinking event tapped");
    }

    private void handleOptInEvent(OptInEvent event) {
        logger.debug("Handling OptInEvent");
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);
        final String recipientId = event.recipientId();
        logger.debug("recipientId: {}", recipientId);
        final String passThroughParam = event.refPayload().orElse("empty payload");
        logger.debug("passThroughParam: {}", passThroughParam);
        final Instant timestamp = event.timestamp();
        logger.debug("timestamp: {}", timestamp);

        logger.info("Received authentication for user '{}' and page '{}' with pass through param '{}' at '{}'", senderId, recipientId, passThroughParam,
                timestamp);
        sendTextMessage(senderId, "Authentication successful");
    }

    private void handleMessageEchoEvent(MessageEchoEvent event) {
        logger.debug("Handling MessageEchoEvent");
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);
        final String recipientId = event.recipientId();
        logger.debug("recipientId: {}", recipientId);
        final String messageId = event.messageId();
        logger.debug("messageId: {}", messageId);
        final Instant timestamp = event.timestamp();
        logger.debug("timestamp: {}", timestamp);

        logger.info("Received echo for message '{}' that has been sent to recipient '{}' by sender '{}' at '{}'", messageId, recipientId, senderId, timestamp);
        sendTextMessage(senderId, "MessageEchoEvent tapped");
    }

    private void handleMessageDeliveredEvent(MessageDeliveredEvent event) {
        logger.debug("Handling MessageDeliveredEvent");
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);
        final List<String> messageIds = event.messageIds().orElse(Collections.emptyList());
        final Instant watermark = event.watermark();
        logger.debug("watermark: {}", watermark);

        messageIds.forEach(messageId -> {
            logger.info("Received delivery confirmation for message '{}'", messageId);
        });

        logger.info("All messages before '{}' were delivered to user '{}'", watermark, senderId);
    }

    private void handleMessageReadEvent(MessageReadEvent event) {
        logger.debug("Handling MessageReadEvent");
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);
        final Instant watermark = event.watermark();
        logger.debug("watermark: {}", watermark);

        logger.info("All messages before '{}' were read by user '{}'", watermark, senderId);
    }

    private void handleFallbackEvent(Event event) {
        logger.debug("Handling FallbackEvent");
        final String senderId = event.senderId();
        logger.debug("senderId: {}", senderId);

        logger.info("Received unsupported message from user '{}'", senderId);
    }

/*    private void sendTextMessageWithType(String recipientId, String text, String type, String step) {
        try {
            final IdRecipient recipient = IdRecipient.create(recipientId);
            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "DEVELOPER_DEFINED_METADATA";

            final TextMessage textMessage = TextMessage.create(text, empty(), of(metadata));
            final MessagePayload messagePayload = MessagePayload.create(recipient, MessagingType.RESPONSE, textMessage,
                    of(notificationType), empty());
            this.messenger.send(messagePayload);

            userSessionMaps.get(recipientId).setType(type);
            userSessionMaps.get(recipientId).setStep(step);
            if("review".equals(step)) {
                final PostbackButton buttonB1 = PostbackButton.create("useful", "useful");
                final PostbackButton buttonB2 = PostbackButton.create("useless", "useless");

                final List<Button> buttons = Arrays.asList(buttonB1, buttonB2);
                final ButtonTemplate buttonTemplate =
                        ButtonTemplate.create("Is this tip usefull or useless?", buttons);

                final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
                final MessagePayload payload1 =
                        MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
                messenger.send(payload1);
            }
        } catch (MessengerApiException | MessengerIOException e) {
            handleSendException(e);
        }
    }*/

    private void  sendEndMessage(String recipientId, String text) {
        try {

            final IdRecipient recipient = IdRecipient.create(recipientId);
            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "DEVELOPER_DEFINED_METADATA";

            final TextMessage textMessage = TextMessage.create(text, empty(), of(metadata));
            final MessagePayload messagePayload = MessagePayload.create(recipient, MessagingType.RESPONSE, textMessage,
                    of(notificationType), empty());
            this.messenger.send(messagePayload);
        } catch (MessengerApiException | MessengerIOException e) {
            handleSendException(e);
        }
    }

    private void  sendAbuseMessage(String recipientId, String text) {
        try {
            final IdRecipient recipient = IdRecipient.create(recipientId);
            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "DEVELOPER_DEFINED_METADATA";

            final TextMessage textMessage = TextMessage.create(text, empty(), of(metadata));
            final MessagePayload messagePayload = MessagePayload.create(recipient, MessagingType.RESPONSE, textMessage,
                    of(notificationType), empty());
            this.messenger.send(messagePayload);
        } catch (MessengerApiException | MessengerIOException e) {
            handleSendException(e);
        }
    }


    private void sendTextMessage(String recipientId, String text) {
        try {
            final IdRecipient recipient = IdRecipient.create(recipientId);
            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "DEVELOPER_DEFINED_METADATA";

            final TextMessage textMessage = TextMessage.create(text, empty(), of(metadata));
            final MessagePayload messagePayload = MessagePayload.create(recipient, MessagingType.RESPONSE, textMessage,
                    of(notificationType), empty());
            this.messenger.send(messagePayload);
        } catch (MessengerApiException | MessengerIOException e) {
            handleSendException(e);
        }
    }

    private void handleSendException(Exception e) {
        logger.error("Message could not be sent. An unexpected error occurred.", e);
    }
}
