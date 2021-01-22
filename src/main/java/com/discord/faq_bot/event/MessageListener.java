package com.discord.faq_bot.event;

import com.discord.faq_bot.CommandRepository;
import com.discord.faq_bot.CustomCommand;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.entity.RestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class MessageListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CommandRepository commandRepository;

    public Mono<Void> processCommand(Message eventMessage){

        Optional<User> author = eventMessage.getAuthor();
        String content = eventMessage.getContent().toLowerCase();

        if(author.isPresent()){
            User user = author.get();
            logger.info("Message Owner ID " + user.getId());
            logger.info("Mention : " + user.getMention());

            if(!user.isBot()){

                String resp = "";

                List<CustomCommand> all = commandRepository.findAll();

                logger.info("All Message in Repo : " + all.size());

                if(content.contains("!list-cmd")){
                    resp = all.stream().map(CustomCommand::getIdentifier).collect(Collectors.joining("\n"));
                    if(resp.length()>0){
                        resp = "List of available commands are : \n" + resp;
                    }
                }else if(content.startsWith("!add-cmd")){
                    String[] splittedContent = content.split(" ");
                    if(splittedContent[0].equals("!add-cmd") && splittedContent.length >3){
                        String identifier = splittedContent[1].toLowerCase();
                        List<CustomCommand> cmdList = commandRepository.getByIdentifierEquals(identifier);
                        if(cmdList.size()==0){
                            CustomCommand customCommand = new CustomCommand(identifier,content.substring(splittedContent[0].length() + splittedContent[1].length()+1),user.getId().asLong(),false);
                            commandRepository.save(customCommand);
                            resp = "Command added Successfully";
                        }else {
                            resp = cmdList.size() + " existing command found with name : "+identifier;
                        }
                    }else{
                        resp = "For using !add cmd message should be in below format \n !add <trigger_name> <message>";
                    }
                }else{

                    int i = content.indexOf(" ");
                    if(i != -1){
                        Set<String> dbIdentifier = all.stream().map(CustomCommand::getIdentifier).collect(Collectors.toSet());

                        Set<String> collect1 = Arrays.stream(content.split(" ")).collect(Collectors.toSet());
                        for (String c : collect1) {
                            if(dbIdentifier.contains(c.toLowerCase())){
                                List<CustomCommand> byIdentifierEquals = commandRepository.getByIdentifierEquals(c);
                                CustomCommand customCommand = byIdentifierEquals.get(0);
                                resp = customCommand.getMessage();
                            }
                        }
                    }else{
                        Set<String> dbIdentifier = all.stream().map(CustomCommand::getIdentifier).collect(Collectors.toSet());
                        for (String s : dbIdentifier) {
                            if (s.contains(content.toLowerCase())){
                                List<CustomCommand> byIdentifierEquals = commandRepository.getByIdentifierEquals(content.toLowerCase());
                                CustomCommand customCommand = byIdentifierEquals.get(0);
                                resp = customCommand.getMessage();
                            }
                        }
                    }
                }

                if (resp.length() != 0){
                    return eventMessage
                            .getRestChannel()
                            .createMessage(resp)
                            .then();
                }

                /*if(content.contains("hi")){
                    return eventMessage
                            .getRestChannel()
                            .createMessage("Hi Bye Chodiye !! Vada ki Jai Boliye :p")
                            .then();

                }else if( content.contains("join") && content.contains("clan")){
                    return eventMessage
                            .getRestChannel()
                            .createMessage("Ahh of course you can join our clan first sub to <@!731395055839084604> YT channel??")
                            .then();
                }else if( content.contains("!list-cmds")){

                }*/

            }
        }

        return Mono.empty();
        /*return Mono.just(eventMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(message -> message.getContent().contains("joidasn") && message.getContent().contains("sda"))
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("<@521994349265813514> Bot is OP !! <@!731395055839084604> Server OooPee"))
                .then();*/
    }

}
