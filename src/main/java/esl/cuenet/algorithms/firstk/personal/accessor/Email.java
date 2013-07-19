package esl.cuenet.algorithms.firstk.personal.accessor;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import esl.cuenet.algorithms.firstk.personal.EventContextNetwork;
import esl.cuenet.algorithms.firstk.personal.Location;
import esl.cuenet.algorithms.firstk.personal.Time;
import esl.cuenet.query.drivers.mongodb.MongoDB;
import esl.cuenet.source.accessors.Utils;
import org.apache.log4j.Logger;

import javax.mail.internet.MailDateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Email implements Source {

    private Logger logger = Logger.getLogger(Email.class);
    private SimpleDateFormat rfc2822DateFormatter = new MailDateFormat();
    private final List<EmailObject> emails;
    private Candidates candidateList = Candidates.getInstance();


    protected Email() {
        MailLoader loader = new MailLoader();
        this.emails = loader.load();
        logger.info("Loaded " + emails.size() + " emails.");
    }

    private static Email instance = new Email();
    public static Email getInstance() {
        return instance;
    }

    @Override
    public List<EventContextNetwork> eventsContaining(Candidates.CandidateReference person, Time interval, Location location) {
        return null;
    }

    @Override
    public List<EventContextNetwork> participants(EventContextNetwork.Event event) {
        return null;
    }

    @Override
    public List<EventContextNetwork> subevents(EventContextNetwork.Event event) {
        return null;
    }

    @Override
    public List<Candidates.CandidateReference> knows(Candidates.CandidateReference person) {
        return null;
    }

    @Override
    public List<EventContextNetwork> knowsAtTime(Candidates.CandidateReference person, Time time) {
        if ( !time.isMoment() ) throw new RuntimeException("time should be a moment");
        HashSet<Candidates.CandidateReference> candidates = new HashSet<Candidates.CandidateReference>();

        long msGap = (long) 120 * 3600 * 1000;
        Time start = time.subtract(msGap);
        Time end = time.add(msGap);

        System.out.println(new Date(start.getStart()) + " " + new Date(end.getStart()));

        List<EventContextNetwork> events = Lists.newArrayList();
        for (EmailObject email: emails) {
            if ( start.isBefore(email.time) && email.time.isBefore(end) ) {
                ///System.out.println(email.nameMailPairs + " " + new Date(email.time.getStart()));
                EventContextNetwork network = new EventContextNetwork();
                EventContextNetwork.ECNRef mailRef = network.createEvent("email-exchange-event", email.time.getStart(), email.time.getEnd());

                candidates.addAll(email.references);
                for (Candidates.CandidateReference ref: candidates) {
                    network.createPartiticipationEdge(mailRef, network.createPerson(ref));
                }

                events.add(network);
            }
        }

        return events;
    }


    public class EmailObject {
        List<Map.Entry<String, String>> nameMailPairs;
        List<Candidates.CandidateReference> references;
        Time time;
    }

    public class MailLoader extends MongoDB {

        public MailLoader() {
            super(PConstants.DBNAME);
        }

        public List<EmailObject> load() {
            MongoDB.DBReader reader = startReader("emails");
            reader.getAll(new BasicDBObject());

            String to, from, cc, date;
            List<EmailObject> emails = new ArrayList<EmailObject>();
            while (reader.hasNext()) {
                BasicDBObject obj = (BasicDBObject) reader.next();

                EmailObject email = new EmailObject();
                email.nameMailPairs = Lists.newArrayList();

                to = obj.getString("to");
                if (to != null) email.nameMailPairs.addAll(Utils.parseEmailAddresses(to));

                from = obj.getString("from");
                if (from != null) email.nameMailPairs.addAll(Utils.parseEmailAddresses(from));

                cc = obj.getString("cc");
                if (cc != null) email.nameMailPairs.addAll(Utils.parseEmailAddresses(cc));

                date = obj.getString("date");
                email.time = getDate(date);

                checkCandidates(email);

                emails.add(email);
            }
            close();
            return emails;
        }

        private void checkCandidates(EmailObject emailObject) {
            emailObject.references = Lists.newArrayList();
            for (Map.Entry<String, String> pair: emailObject.nameMailPairs) {
                String email = pair.getKey().toLowerCase();
                String name = pair.getValue();

                if (name != null) name = name.toLowerCase();

                Candidates.CandidateReference cReference = null;
                cReference = candidateList.search(Candidates.EMAIL_KEY, email);
                if (cReference == Candidates.UNKNOWN && name != null) cReference = candidateList.search(Candidates.NAME_KEY, name);

                if (cReference == Candidates.UNKNOWN) candidateList.createCandidate(Candidates.EMAIL_KEY, email);
                candidateList.add(cReference, Candidates.EMAIL_KEY, email);
                if (name != null) candidateList.add(cReference, Candidates.NAME_KEY, name);

                if ( !emailObject.references.contains(cReference) ) emailObject.references.add(cReference);
            }
        }

        private Time getDate(String date) {
            long ms = 0;
            if (date != null) {
                try {
                    ms = (rfc2822DateFormatter.parse(date)).getTime();
                } catch (ParseException e) {
                    logger.error("RFC2822 Date Parsing failed: " + date + " " + e.getMessage());
                }
            } else {
                logger.info("Date is null!");
            }

            return Time.createFromMoment(ms);
        }
    }
}
