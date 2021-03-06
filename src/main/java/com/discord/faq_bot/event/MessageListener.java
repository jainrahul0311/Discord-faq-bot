package com.discord.faq_bot.event;

import com.discord.faq_bot.CommandRepository;
import com.discord.faq_bot.CustomCommand;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class MessageListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CommandRepository commandRepository;

    public Mono<Void> processCommand(Message eventMessage) {

        Optional<User> author = eventMessage.getAuthor();
        try {

            String content = eventMessage.getContent().toLowerCase();

            if (author.isPresent()) {
                User user = author.get();
                logger.info("Message : " + content);

                if (!user.isBot()) {

                    String resp = "";

                    List<CustomCommand> all = commandRepository.findAll();

                    if (content.contains("!list-cmd")) {
                        String tempResp = "Special Non-Removable Commands are : \n\n!add-cmd\n!rm-cmd\n\n";
                        resp = all.stream().map(CustomCommand::getIdentifier).collect(Collectors.joining("\n"));
                        if (resp.length() > 0) {
                            resp = "List of Custom Commands are : \n\n" + resp;
                        }
                        resp = tempResp + resp;
                    } else if (content.contains("!add-cmd")) {

                        String[] splittedContent = content.split(" ");
                        if (splittedContent[0].equals("!add-cmd") && splittedContent.length >= 3) {
                            String identifier = splittedContent[1];
                            List<CustomCommand> cmdList = commandRepository.getByIdentifierEquals(identifier);
                            if (cmdList.size() == 0) {
                                CustomCommand customCommand = new CustomCommand(identifier, content.substring(splittedContent[0].length() + splittedContent[1].length() + 1), user.getId().asLong(), false);
                                commandRepository.save(customCommand);
                                resp = "Command added Successfully";
                            } else {
                                resp = cmdList.size() + " existing command found with name : " + identifier;
                            }
                        } else {
                            resp = "For using !add cmd message should be in below format \n !add <trigger_name> <message>";
                        }
                    } else if (content.contains("!rm-cmd")) {
                        String[] splittedContent = content.split(" ");
                        if (splittedContent[0].equals("!rm-cmd") && splittedContent.length == 2) {
                            String identifier = splittedContent[1];
                            List<CustomCommand> cmdList = commandRepository.getByIdentifierEquals(identifier);
                            if (cmdList.size() > 0) {
                                CustomCommand customCommand = cmdList.get(0);
                                commandRepository.delete(customCommand);
                                resp = identifier + " Command was removed Successfully";
                            } else {
                                resp = "No existing Command found with name : " + identifier;
                            }
                        } else {
                            resp = "For using !rm-cmd Command message should be in below format \n !rm-cmd <trigger_name>";
                        }
                    } else {
                        int i = content.indexOf(" ");
                        if (i != -1) {
                            Set<String> dbIdentifier = all.stream().map(CustomCommand::getIdentifier).collect(Collectors.toSet());

                            Set<String> collect1 = Arrays.stream(content.split(" ")).collect(Collectors.toSet());
                            for (String c : collect1) {
                                if (dbIdentifier.contains(c)) {
                                    List<CustomCommand> byIdentifierEquals = commandRepository.getByIdentifierEquals(c);
                                    CustomCommand customCommand = byIdentifierEquals.get(0);
                                    resp = customCommand.getMessage();
                                }
                            }
                        } else if (content.length() != 0) {
                            Set<String> dbIdentifier = all.stream().map(CustomCommand::getIdentifier).collect(Collectors.toSet());
                            for (String s : dbIdentifier) {
                                if (s.contains(content)) {
                                    List<CustomCommand> byIdentifierEquals = commandRepository.getByIdentifierEquals(content);
                                    CustomCommand customCommand = byIdentifierEquals.get(0);
                                    resp = customCommand.getMessage();
                                }
                            }
                        }
                    }

                    if (resp.length() != 0) {
                        String finalResp = resp;

                        return Mono.just(eventMessage)
                                .flatMap(Message::getChannel)
                                .flatMap(channel -> channel
                                        .createEmbed(
                                                spec -> spec
                                                        .setColor(Color.GREEN)
                                                        .setAuthor("F.A.Q - BOT", "", "https://www.cookwithmanali.com/wp-content/uploads/2018/04/Vada-Pav.jpg")
                                                        .setDescription("`" + finalResp + "`")
                                                        .setTimestamp(Instant.now())
                                        )
                                )
                                .then();
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Exception inside MessageListener : " + e.getMessage());
            e.printStackTrace();
        }
        return Mono.empty();
    }

}
