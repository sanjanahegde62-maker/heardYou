package com.heardYou.config;

import com.heardYou.condition.Condition;
import com.heardYou.condition.ConditionRepository;
import com.heardYou.condition.ConditionSymptom;
import com.heardYou.condition.ConditionSymptomRepository;
import com.heardYou.source.MedicalSource;
import com.heardYou.source.MedicalSourceRepository;
import com.heardYou.symptom.Symptom;
import com.heardYou.symptom.SymptomRepository;
import com.heardYou.warning.WarningSign;
import com.heardYou.warning.WarningSignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Seeds the database with carefully curated educational data on first run.
 * All seeded data is skipped if records already exist.
 *
 * Sources cited here are real, reputable organisations. Do not add medical
 * claims that have not been verified against these sources. Claims marked
 * needsEditorialReview=true are placeholders pending editorial confirmation.
 */
@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final SymptomRepository symptomRepo;
    private final ConditionRepository conditionRepo;
    private final ConditionSymptomRepository csRepo;
    private final MedicalSourceRepository sourceRepo;
    private final WarningSignRepository warningSignRepo;

    public DataLoader(SymptomRepository symptomRepo,
                      ConditionRepository conditionRepo,
                      ConditionSymptomRepository csRepo,
                      MedicalSourceRepository sourceRepo,
                      WarningSignRepository warningSignRepo) {
        this.symptomRepo = symptomRepo;
        this.conditionRepo = conditionRepo;
        this.csRepo = csRepo;
        this.sourceRepo = sourceRepo;
        this.warningSignRepo = warningSignRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (symptomRepo.count() > 0) {
            log.info("DataLoader: core data already present, checking for incremental additions...");
            // Always re-check warning signs so new rules can be added without a full reseed
            incrementalAliasUpdates();
            seedWarningSigns();
            return;
        }
        log.info("DataLoader: seeding initial educational data...");
        seedSources();
        seedSymptoms();
        seedConditions();
        seedWarningSigns();
        log.info("DataLoader: seed complete.");
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // SOURCES
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private Map<String, MedicalSource> sources = new LinkedHashMap<>();

    private void seedSources() {
        sources.put("endo-nhs", makeSource(
                "Endometriosis Ã¢â‚¬â€œ NHS",
                "NHS (National Health Service, UK)",
                "https://www.nhs.uk/conditions/endometriosis/",
                LocalDate.of(2023, 1, 1),
                List.of("Endometriosis affects roughly 1 in 10 women of reproductive age.",
                        "Common symptoms include pelvic pain and heavy periods."),
                false));

        sources.put("endo-who", makeSource(
                "Endometriosis Ã¢â‚¬â€œ World Health Organization",
                "World Health Organization",
                "https://www.who.int/news-room/fact-sheets/detail/endometriosis",
                LocalDate.of(2023, 3, 1),
                List.of("Endometriosis is a chronic disease with no cure, but treatment can help manage symptoms.",
                        "Diagnosis is often delayed by years."),
                false));

        sources.put("pcos-nhs", makeSource(
                "Polycystic ovary syndrome (PCOS) Ã¢â‚¬â€œ NHS",
                "NHS (National Health Service, UK)",
                "https://www.nhs.uk/conditions/polycystic-ovary-syndrome-pcos/",
                LocalDate.of(2022, 11, 1),
                List.of("PCOS affects how the ovaries work.",
                        "Common symptoms include irregular periods, excess androgen, and polycystic ovaries."),
                false));

        sources.put("pcos-who", makeSource(
                "Polycystic ovary syndrome Ã¢â‚¬â€œ World Health Organization",
                "World Health Organization",
                "https://www.who.int/news-room/fact-sheets/detail/polycystic-ovary-syndrome",
                LocalDate.of(2023, 6, 28),
                List.of("PCOS affects an estimated 8Ã¢â‚¬â€œ13% of women of reproductive age.",
                        "Up to 70% of affected women remain undiagnosed globally."),
                false));

        sources.put("fibro-nhs", makeSource(
                "Fibromyalgia Ã¢â‚¬â€œ NHS",
                "NHS (National Health Service, UK)",
                "https://www.nhs.uk/conditions/fibromyalgia/",
                LocalDate.of(2023, 1, 1),
                List.of("Fibromyalgia causes widespread musculoskeletal pain, fatigue, and sleep disturbance.",
                        "It is more commonly diagnosed in women."),
                false));

        sources.put("adenomyosis-nhs", makeSource(
                "Adenomyosis Ã¢â‚¬â€œ NHS",
                "NHS (National Health Service, UK)",
                "https://www.nhs.uk/conditions/adenomyosis/",
                LocalDate.of(2022, 8, 1),
                List.of("Adenomyosis is where the tissue that normally lines the womb grows into the womb's muscular wall.",
                        "Symptoms include heavy, painful periods and pelvic pain."),
                false));

        sources.put("pmdd-nice", makeSource(
                "Premenstrual dysphoric disorder (PMDD) Ã¢â‚¬â€œ NICE CKS",
                "NICE (National Institute for Health and Care Excellence)",
                "https://cks.nice.org.uk/topics/premenstrual-syndrome/",
                LocalDate.of(2023, 1, 1),
                List.of("PMDD is a severe form of PMS with significant psychological and physical symptoms."),
                false));

        sources.put("thyroid-nhs", makeSource(
                "Underactive thyroid (hypothyroidism) Ã¢â‚¬â€œ NHS",
                "NHS (National Health Service, UK)",
                "https://www.nhs.uk/conditions/underactive-thyroid-hypothyroidism/",
                LocalDate.of(2022, 7, 1),
                List.of("Hypothyroidism is more common in women, especially over 60.",
                        "Symptoms include tiredness, weight gain, depression, and feeling cold."),
                false));

        sources.put("heart-aha", makeSource(
                "Heart Attack Symptoms in Women Ã¢â‚¬â€œ American Heart Association",
                "American Heart Association",
                "https://www.heart.org/en/health-topics/heart-attack/warning-signs-of-a-heart-attack/heart-attack-symptoms-in-women",
                LocalDate.of(2023, 1, 1),
                List.of("Women are more likely than men to experience heart attack symptoms other than chest pain.",
                        "Symptoms can include shortness of breath, nausea, jaw pain, and back pain."),
                false));

        sources.put("lupus-nhs", makeSource(
                "Lupus Ã¢â‚¬â€œ NHS",
                "NHS (National Health Service, UK)",
                "https://www.nhs.uk/conditions/lupus/",
                LocalDate.of(2022, 10, 1),
                List.of("Lupus is an autoimmune condition that can affect many parts of the body.",
                        "It is much more common in women than men."),
                false));

        sources.put("anxiety-nhs", makeSource(
                "Generalised anxiety disorder Ã¢â‚¬â€œ NHS",
                "NHS (National Health Service, UK)",
                "https://www.nhs.uk/mental-health/conditions/generalised-anxiety-disorder/overview/",
                LocalDate.of(2023, 5, 1),
                List.of("Anxiety disorders are more common in women than men.",
                        "Symptoms include persistent worry, physical tension, and sleep disturbance."),
                false));

        sources.put("migraine-nhs", makeSource(
                "Migraine Ã¢â‚¬â€œ NHS",
                "NHS (National Health Service, UK)",
                "https://www.nhs.uk/conditions/migraine/",
                LocalDate.of(2022, 12, 1),
                List.of("Migraines are more common in women, often linked to hormonal changes.",
                        "Symptoms include severe headache, nausea, and sensitivity to light."),
                false));

        sourceRepo.saveAll(sources.values());
    }

    private MedicalSource makeSource(String title, String publisher, String url,
                                      LocalDate reviewDate, List<String> claims,
                                      boolean needsReview) {
        MedicalSource s = new MedicalSource();
        s.setTitle(title);
        s.setPublisher(publisher);
        s.setUrl(url);
        s.setReviewDate(reviewDate);
        s.setSupportedClaims(new ArrayList<>(claims));
        s.setNeedsEditorialReview(needsReview);
        return s;
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // SYMPTOMS
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private Map<String, Symptom> symptoms = new LinkedHashMap<>();

    private void seedSymptoms() {
        symptoms.put("pelvic-pain", makeSymptom("Pelvic pain", "pelvic-pain",
                "Pain felt in the lower abdomen and pelvic region.", "pain",
                "lower abdominal pain", "period pain", "pelvic cramps", "womb pain"));
        symptoms.put("painful-periods", makeSymptom("Painful periods (dysmenorrhoea)", "painful-periods",
                "Cramping pain during menstruation, which may be severe.", "pain",
                "dysmenorrhoea", "menstrual cramps", "period cramps", "period pain"));
        symptoms.put("heavy-periods", makeSymptom("Heavy periods (menorrhagia)", "heavy-periods",
                "Unusually heavy or prolonged menstrual bleeding.", "hormonal",
                "menorrhagia", "heavy bleeding", "flooding periods", "excessive bleeding"));
        symptoms.put("irregular-periods", makeSymptom("Irregular periods", "irregular-periods",
                "Menstrual cycles that are unpredictable in timing or length.", "hormonal",
                "missed periods", "absent periods", "oligomenorrhoea", "anovulation", "skipped periods"));
        symptoms.put("fatigue", makeSymptom("Fatigue", "fatigue",
                "Persistent tiredness or exhaustion not relieved by rest.", "systemic",
                "tiredness", "exhaustion", "lethargy", "no energy", "always tired"));
        symptoms.put("bloating", makeSymptom("Bloating", "bloating",
                "A feeling of fullness or swelling in the abdomen.", "gastrointestinal",
                "abdominal bloating", "stomach bloating", "distension", "swollen abdomen"));
        symptoms.put("nausea", makeSymptom("Nausea", "nausea",
                "A sensation of unease in the stomach with urge to vomit.", "gastrointestinal",
                "feeling sick", "sick stomach", "queasiness"));
        symptoms.put("pain-during-sex", makeSymptom("Pain during sex (dyspareunia)", "pain-during-sex",
                "Pain felt during or after sexual intercourse.", "pain",
                "dyspareunia", "painful intercourse", "sex pain"));
        symptoms.put("lower-back-pain", makeSymptom("Lower back pain", "lower-back-pain",
                "Pain in the lumbar region, which may be chronic or episodic.", "pain",
                "back pain", "lumbar pain", "lumbago"));
        symptoms.put("acne", makeSymptom("Acne", "acne",
                "Spots, pimples, or cysts on the face, chest, or back.", "hormonal",
                "spots", "pimples", "breakouts", "skin blemishes"));
        symptoms.put("excess-hair-growth", makeSymptom("Excess hair growth (hirsutism)", "excess-hair-growth",
                "Unwanted, male-pattern hair growth on the face, chest, or other areas.", "hormonal",
                "hirsutism", "unwanted hair", "facial hair", "body hair"));
        symptoms.put("hair-thinning", makeSymptom("Hair thinning or loss", "hair-thinning",
                "Noticeable thinning of scalp hair or hair loss.", "hormonal",
                "hair loss", "alopecia", "thinning hair", "hair falling out"));
        symptoms.put("weight-gain", makeSymptom("Unexplained weight gain", "weight-gain",
                "Weight gain without an obvious change in diet or activity.", "metabolic",
                "gaining weight", "unexplained weight increase", "putting on weight"));
        symptoms.put("difficulty-conceiving", makeSymptom("Difficulty conceiving", "difficulty-conceiving",
                "Difficulty becoming pregnant after regular unprotected sex.", "reproductive",
                "infertility", "fertility problems", "not getting pregnant", "subfertility"));
        symptoms.put("widespread-pain", makeSymptom("Widespread musculoskeletal pain", "widespread-pain",
                "Pain across multiple areas of the body, often including muscles and joints.", "pain",
                "body pain", "muscle pain", "diffuse pain", "all over pain", "fibro pain"));
        symptoms.put("sleep-disturbance", makeSymptom("Sleep disturbance", "sleep-disturbance",
                "Difficulty falling asleep, staying asleep, or feeling rested.", "neurological",
                "insomnia", "can't sleep", "poor sleep", "waking up at night", "unrefreshing sleep"));
        symptoms.put("mood-changes", makeSymptom("Mood changes or irritability", "mood-changes",
                "Noticeable changes in mood, including depression or anxiety, particularly cycle-related.", "psychological",
                "irritability", "mood swings", "depression", "low mood", "emotional changes"));
        symptoms.put("cognitive-fog", makeSymptom("Cognitive fog (difficulty concentrating)", "cognitive-fog",
                "Difficulty thinking clearly, concentrating, or remembering things.", "neurological",
                "brain fog", "fibro fog", "difficulty concentrating", "memory problems", "forgetfulness"));
        symptoms.put("bowel-changes", makeSymptom("Bowel changes related to periods", "bowel-changes",
                "Diarrhoea, constipation, or pain with bowel movements, particularly during menstruation.", "gastrointestinal",
                "IBS", "diarrhoea", "diarrhea", "loose stools", "painful bowel movements", "bowel pain", "stomach pain", "abdominal pain", "tummy pain", "gut pain"));
        symptoms.put("urinary-symptoms", makeSymptom("Urinary symptoms", "urinary-symptoms",
                "Increased urgency to urinate, pain on urination, or blood in urine.", "urological",
                "urinary urgency", "pain urinating", "frequent urination", "blood in urine", "haematuria"));
        symptoms.put("cold-intolerance", makeSymptom("Cold intolerance", "cold-intolerance",
                "Feeling unusually sensitive to cold temperatures.", "metabolic",
                "feeling cold", "sensitivity to cold", "always cold"));
        symptoms.put("dry-skin", makeSymptom("Dry skin or hair", "dry-skin",
                "Unusually dry skin or brittle hair.", "metabolic",
                "dry hair", "brittle hair", "rough skin", "flaky skin"));
        symptoms.put("constipation", makeSymptom("Constipation", "constipation",
                "Infrequent or difficult bowel movements.", "gastrointestinal",
                "difficulty going to the toilet", "hard stools", "infrequent bowel movements"));
        symptoms.put("headache", makeSymptom("Headache", "headache",
                "Pain or pressure in the head, which may be frequent or chronic.", "neurological",
                "head pain", "migraine", "tension headache", "head pressure"));
        // New symptoms for cardiovascular, autoimmune, brain/mind conditions
        symptoms.put("chest-discomfort", makeSymptom("Chest discomfort or pressure", "chest-discomfort",
                "A feeling of pressure, tightness, squeezing, or pain in the chest.", "cardiovascular",
                "chest pain", "chest tightness", "chest pressure", "chest squeezing"));
        symptoms.put("shortness-of-breath", makeSymptom("Shortness of breath", "shortness-of-breath",
                "Difficulty breathing or feeling breathless, at rest or with mild exertion.", "cardiovascular",
                "breathless", "difficulty breathing", "can't breathe", "dyspnoea"));
        symptoms.put("jaw-neck-pain", makeSymptom("Jaw, neck, or back pain", "jaw-neck-pain",
                "Unusual pain in the jaw, neck, shoulder, or upper back.", "cardiovascular",
                "jaw pain", "neck pain", "shoulder pain", "upper back pain"));
        symptoms.put("dizziness", makeSymptom("Dizziness or lightheadedness", "dizziness",
                "Feeling faint, dizzy, or unsteady.", "neurological",
                "lightheaded", "feeling faint", "vertigo", "unsteady", "spinning"));
        symptoms.put("joint-pain", makeSymptom("Joint pain or swelling", "joint-pain",
                "Pain, stiffness, or swelling in one or more joints.", "musculoskeletal",
                "arthritis", "joint swelling", "joint stiffness", "swollen joints"));
        symptoms.put("rash", makeSymptom("Skin rash", "rash",
                "A change in skin colour, texture, or appearance, including butterfly rash on the face.", "dermatological",
                "skin rash", "butterfly rash", "malar rash", "skin changes", "hives"));
        symptoms.put("anxiety", makeSymptom("Persistent anxiety or worry", "anxiety",
                "Excessive, ongoing worry or fear that is difficult to control.", "psychological",
                "worry", "anxious", "panic", "fear", "generalised anxiety", "GAD"));
        symptoms.put("palpitations", makeSymptom("Heart palpitations", "palpitations",
                "Noticeable heartbeats Ã¢â‚¬â€ fast, fluttering, or pounding sensations in the chest.", "cardiovascular",
                "racing heart", "heart racing", "irregular heartbeat", "fluttering heart", "pounding heart"));
        symptoms.put("light-sensitivity", makeSymptom("Sensitivity to light or sound", "light-sensitivity",
                "Unusual discomfort caused by bright light, loud noise, or strong smells.", "neurological",
                "photophobia", "photosensitivity", "sensitive to light", "light bothers me"));

        symptomRepo.saveAll(symptoms.values());
    }

    private Symptom makeSymptom(String name, String slug, String description, String category,
                                 String... aliases) {
        Symptom s = new Symptom();
        s.setName(name);
        s.setSlug(slug);
        s.setDescription(description);
        s.setCategory(category);
        if (aliases.length > 0) {
            s.setAliases(new ArrayList<>(List.of(aliases)));
        }
        return s;
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // CONDITIONS
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private void seedConditions() {
        // REPRODUCTIVE body system
        seedEndometriosis();
        seedAdenomyosis();
        seedPcos();
        seedPmdd();
        // HORMONAL_METABOLIC body system
        seedHypothyroidism();
        // CARDIOVASCULAR body system
        seedHeartDiseaseInWomen();
        // BRAIN_MIND body system
        seedAnxietyDisorder();
        seedMenstrualMigraine();
        // AUTOIMMUNE body system
        seedLupus();
        // BONE_PAIN body system
        seedFibromyalgia();
    }

    private void seedEndometriosis() {
        Condition c = new Condition();
        c.setName("Endometriosis");
        c.setSlug("endometriosis");
        c.setBodySystem("REPRODUCTIVE");
        c.setSummary("Endometriosis is a condition where tissue similar to the lining of the uterus grows outside the uterus, often causing pain and other symptoms.");
        c.setMedicalDescription(
                "Endometriosis involves ectopic endometrial-like tissue found outside the uterus, " +
                "most commonly on the ovaries, fallopian tubes, and pelvic peritoneum. " +
                "It is an estrogen-dependent inflammatory condition.");
        c.setConventionalPresentation(
                "Textbooks often describe endometriosis as presenting with the triad of: " +
                "dysmenorrhoea (painful periods), dyspareunia (pain during sex), and infertility.");
        c.setFemalePresentation(
                "Many people describe years of being told their pain is 'just bad periods'. " +
                "Gastrointestinal symptoms, bladder symptoms, and fatigue are often prominent but not " +
                "always recognised as part of endometriosis. Pain does not always correlate with disease extent.");
        c.setDocumentedPresentations(List.of(
                "Many people experience years of diagnostic delay, with symptoms dismissed or attributed to normal menstruation.",
                "Gastrointestinal symptoms such as painful bowel movements during menstruation are frequently reported but often overlooked.",
                "Fatigue is commonly reported and may be severe, but is not always included in standard diagnostic criteria.",
                "Bladder symptoms and urinary pain can occur, particularly with bladder endometriosis.",
                "Symptoms do not always correlate with the extent of endometriosis found at surgery."));
        c.setCommonlyMistakenFor(List.of("Irritable bowel syndrome (IBS)", "Pelvic inflammatory disease (PID)",
                "Ovarian cysts", "Normal menstrual pain"));
        c.setAvgDiagnosisDelay("7Ã¢â‚¬â€œ10 years on average (WHO, 2023)");
        c.setSuggestedTests(List.of(
                "Pelvic ultrasound (transvaginal)",
                "MRI of the pelvis",
                "Laparoscopy (definitive diagnosis)"));
        c.setTimeframeAdvice(
                "If you have had painful or heavy periods for more than a few months, or pain that affects " +
                "your daily life, discuss this with your GP. A referral to a gynaecologist is appropriate " +
                "if endometriosis is suspected.");
        c.setCategory("reproductive");
        c.setExtraCategories(Set.of("pain", "hormonal"));
        c.setLifeStages(Set.of("reproductive-age", "teen", "perimenopause"));
        c.setSpecialistType("Gynaecologist / Endometriosis specialist");
        c.setSpecialistNotes("A gynaecologist with experience in endometriosis should be involved. Diagnosis is confirmed by laparoscopy.");
        c.setEducationalGuidance(
                "Endometriosis is a real, recognised medical condition. Symptoms being severe does not mean they are normal. " +
                "If you suspect endometriosis, ask your doctor about a referral to a gynaecologist.");
        c.setClinicianQuestions(List.of(
                "Could my symptoms be consistent with endometriosis?",
                "Would a referral to a gynaecologist be appropriate?",
                "Are there imaging tests such as ultrasound or MRI that might be helpful?",
                "What are the options for managing pain?",
                "Could my gastrointestinal symptoms be related to a gynaecological condition?"));
        c.setSources(Set.of(sources.get("endo-nhs"), sources.get("endo-who")));
        conditionRepo.save(c);

        addMapping(c, "pelvic-pain", 0.90, "Pelvic pain is one of the most commonly documented symptoms of endometriosis. (NHS, WHO)");
        addMapping(c, "painful-periods", 0.90, "Dysmenorrhoea (painful periods) is a hallmark symptom. (NHS)");
        addMapping(c, "pain-during-sex", 0.80, "Dyspareunia is a well-documented symptom. (NHS, WHO)");
        addMapping(c, "heavy-periods", 0.65, "Heavy periods are documented in a significant proportion of people with endometriosis. (NHS)");
        addMapping(c, "fatigue", 0.60, "Fatigue is frequently reported by people with endometriosis, though it is not always included in formal criteria. (WHO)");
        addMapping(c, "bowel-changes", 0.65, "Bowel symptoms during menstruation are well-documented in endometriosis. (NHS)");
        addMapping(c, "bloating", 0.55, "Bloating is commonly reported by people with endometriosis. (NHS Ã¢â‚¬â€œ editorial review pending for precise prevalence figure)", true);
        addMapping(c, "lower-back-pain", 0.55, "Lower back pain is documented in endometriosis. (NHS)");
        addMapping(c, "urinary-symptoms", 0.40, "Urinary symptoms can occur with bladder endometriosis. (NHS)");
        addMapping(c, "difficulty-conceiving", 0.50, "Infertility is associated with endometriosis; however, many with endometriosis do conceive. (NHS, WHO)");
        addMapping(c, "nausea", 0.35, "Nausea is reported by some people with endometriosis, particularly during menstruation. (editorial review pending)", true);
    }

    private void seedPcos() {
        Condition c = new Condition();
        c.setName("Polycystic Ovary Syndrome (PCOS)");
        c.setSlug("pcos");
        c.setBodySystem("REPRODUCTIVE");
        c.setSummary("PCOS is a common hormonal condition affecting the ovaries, associated with irregular periods, elevated androgens, and metabolic effects.");
        c.setMedicalDescription(
                "PCOS is characterised by at least two of the three Rotterdam criteria: " +
                "oligo/anovulation, clinical or biochemical signs of hyperandrogenism, and polycystic ovarian morphology on ultrasound.");
        c.setConventionalPresentation(
                "Textbooks describe PCOS using the Rotterdam criteria. Common presentations include " +
                "irregular or absent periods, features of hyperandrogenism (acne, hirsutism, hair loss), " +
                "and polycystic ovaries on ultrasound.");
        c.setFemalePresentation(
                "Many people with PCOS are diagnosed late, or only when seeking fertility help. " +
                "Metabolic symptoms like weight gain and fatigue are often dismissed. " +
                "Not everyone has cysts on their ovaries despite the name.");
        c.setDocumentedPresentations(List.of(
                "PCOS presents very differently between individuals; not everyone with PCOS has cysts on their ovaries.",
                "Many people are diagnosed only when seeking fertility treatment.",
                "Metabolic features such as insulin resistance may be present and are important for long-term health.",
                "Psychological impact including anxiety and depression is documented in people with PCOS.",
                "Up to 70% of affected women remain undiagnosed globally. (WHO)"));
        c.setCommonlyMistakenFor(List.of("Normal hormonal variation", "Hypothyroidism", "Endometriosis"));
        c.setAvgDiagnosisDelay("Up to 70% remain undiagnosed (WHO, 2023); formal delay data varies by country.");
        c.setSuggestedTests(List.of(
                "Hormone blood tests (LH, FSH, testosterone, SHBG, prolactin)",
                "Fasting blood glucose and insulin resistance markers",
                "AMH (anti-MÃƒÂ¼llerian hormone)",
                "Pelvic ultrasound"));
        c.setTimeframeAdvice(
                "If you have had irregular periods, signs of excess androgens (acne, unwanted hair), or " +
                "difficulty conceiving for 6 months or more, discuss PCOS with your GP.");
        c.setCategory("hormonal");
        c.setExtraCategories(Set.of("reproductive", "metabolic"));
        c.setLifeStages(Set.of("teen", "reproductive-age", "perimenopause"));
        c.setSpecialistType("Gynaecologist / Endocrinologist");
        c.setSpecialistNotes("A gynaecologist or endocrinologist can assess for PCOS. Blood tests and ultrasound are typically used.");
        c.setEducationalGuidance(
                "PCOS is a recognised medical condition with a wide range of presentations. " +
                "A formal diagnosis requires clinical assessment. Ask your doctor about hormone blood tests and ultrasound.");
        c.setClinicianQuestions(List.of(
                "Could my symptoms be consistent with PCOS?",
                "Should I have hormone blood tests such as LH, FSH, testosterone, or an AMH test?",
                "Would a pelvic ultrasound be helpful?",
                "Are there any metabolic checks I should have, such as blood glucose or insulin resistance testing?",
                "What are the options for managing my symptoms?"));
        c.setSources(Set.of(sources.get("pcos-nhs"), sources.get("pcos-who")));
        conditionRepo.save(c);

        addMapping(c, "irregular-periods", 0.90, "Irregular or absent periods are a defining feature of PCOS. (NHS, WHO)");
        addMapping(c, "acne", 0.70, "Acne is a documented feature of hyperandrogenism in PCOS. (NHS)");
        addMapping(c, "excess-hair-growth", 0.70, "Hirsutism (excess hair growth) is a documented sign of hyperandrogenism in PCOS. (NHS)");
        addMapping(c, "hair-thinning", 0.60, "Hair thinning is documented in PCOS and associated with elevated androgens. (NHS)");
        addMapping(c, "weight-gain", 0.55, "Weight gain and difficulty managing weight are documented in PCOS. (NHS)");
        addMapping(c, "difficulty-conceiving", 0.65, "Fertility difficulties are a recognised feature of PCOS. (NHS, WHO)");
        addMapping(c, "fatigue", 0.45, "Fatigue is reported by people with PCOS, possibly related to insulin resistance. (editorial review pending)", true);
        addMapping(c, "mood-changes", 0.45, "Mood changes and depression are documented in PCOS. (editorial review pending for precise prevalence)", true);
        addMapping(c, "pelvic-pain", 0.30, "Pelvic discomfort can be reported in PCOS, though it is not a primary diagnostic criterion. (NHS)");
    }

    private void seedFibromyalgia() {
        Condition c = new Condition();
        c.setName("Fibromyalgia");
        c.setSlug("fibromyalgia");
        c.setBodySystem("BONE_PAIN");
        c.setSummary("Fibromyalgia is a long-term condition causing widespread musculoskeletal pain, fatigue, and sleep and cognitive disturbance.");
        c.setMedicalDescription(
                "Fibromyalgia is characterised by widespread pain lasting more than three months, " +
                "often accompanied by fatigue, sleep difficulties, and cognitive symptoms. " +
                "It is more frequently diagnosed in women.");
        c.setConventionalPresentation(
                "Widespread musculoskeletal pain across multiple body areas, persistent fatigue, " +
                "sleep disturbance, and cognitive difficulties (sometimes called 'fibro fog').");
        c.setFemalePresentation(
                "Fibromyalgia is frequently dismissed or attributed to anxiety or depression, especially in women. " +
                "There is no definitive test; diagnosis is clinical, which can mean a long journey to diagnosis. " +
                "Pain severity and distribution can fluctuate significantly.");
        c.setDocumentedPresentations(List.of(
                "Fibromyalgia is frequently dismissed or delayed in diagnosis, particularly in women.",
                "The condition is diagnosed clinically; there is no definitive blood test or imaging finding.",
                "Symptom severity fluctuates; stress, poor sleep, and activity level can influence symptoms.",
                "Fibromyalgia can coexist with other conditions such as endometriosis or autoimmune conditions."));
        c.setCommonlyMistakenFor(List.of("Rheumatoid arthritis", "Lupus", "Hypothyroidism", "Chronic fatigue syndrome"));
        setAvgDiagnosisDelay(c, "Average 2Ã¢â‚¬â€œ5 years from symptom onset to diagnosis (editorial review pending)", true);
        c.setSuggestedTests(List.of(
                "Blood tests to exclude other causes (ESR, CRP, thyroid function, ANA)",
                "Clinical assessment using 2016 fibromyalgia diagnostic criteria"));
        c.setTimeframeAdvice(
                "If you have had widespread pain and fatigue for more than 3 months, discuss this with your GP. " +
                "A referral to a rheumatologist may be appropriate to rule out other causes.");
        c.setCategory("pain");
        c.setExtraCategories(Set.of("neurological", "systemic"));
        c.setLifeStages(Set.of("reproductive-age", "perimenopause", "postmenopause", "adult"));
        c.setSpecialistType("Rheumatologist");
        c.setSpecialistNotes("A rheumatologist can assess for fibromyalgia and rule out other causes of widespread pain.");
        c.setEducationalGuidance(
                "Fibromyalgia is a recognised medical condition. A diagnosis requires ruling out other conditions. " +
                "Management typically involves a combination of medication, physiotherapy, and lifestyle adjustments.");
        c.setClinicianQuestions(List.of(
                "Could my widespread pain and fatigue be consistent with fibromyalgia?",
                "What tests would help to rule out other causes of my symptoms?",
                "Would a referral to a rheumatologist be appropriate?",
                "What treatment or pain management options are available?",
                "Are there non-medication approaches that might help?"));
        c.setSources(Set.of(sources.get("fibro-nhs")));
        conditionRepo.save(c);

        addMapping(c, "widespread-pain", 0.95, "Widespread musculoskeletal pain is the defining feature of fibromyalgia. (NHS)");
        addMapping(c, "fatigue", 0.85, "Persistent fatigue is a core feature of fibromyalgia. (NHS)");
        addMapping(c, "sleep-disturbance", 0.80, "Sleep disturbance is a core feature of fibromyalgia. (NHS)");
        addMapping(c, "cognitive-fog", 0.75, "Cognitive difficulties ('fibro fog') are a well-documented feature. (NHS)");
        addMapping(c, "mood-changes", 0.55, "Depression and anxiety are commonly associated with fibromyalgia. (NHS)");
        addMapping(c, "lower-back-pain", 0.60, "Lower back pain is frequently reported in fibromyalgia. (NHS)");
        addMapping(c, "headache", 0.50, "Headaches are reported by some people with fibromyalgia. (NHS Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "bowel-changes", 0.40, "Bowel symptoms such as irritable bowel are reported in fibromyalgia. (NHS Ã¢â‚¬â€œ editorial review pending)", true);
    }

    private void seedAdenomyosis() {
        Condition c = new Condition();
        c.setName("Adenomyosis");
        c.setSlug("adenomyosis");
        c.setBodySystem("REPRODUCTIVE");
        c.setSummary("Adenomyosis is a condition where tissue similar to the lining of the uterus grows into the muscular wall of the uterus, often causing heavy and painful periods.");
        c.setMedicalDescription(
                "In adenomyosis, endometrial glands and stroma are present within the myometrium. " +
                "It may be focal or diffuse and can enlarge the uterus.");
        c.setConventionalPresentation(
                "Heavy, prolonged, and painful periods, an enlarged uterus, and pelvic pain or pressure.");
        c.setFemalePresentation(
                "Adenomyosis is often only diagnosed after MRI or hysterectomy. It can coexist with " +
                "endometriosis and symptoms may be attributed solely to endometriosis, causing the " +
                "adenomyosis component to be missed. Pain and heavy bleeding can be severe and debilitating.");
        c.setDocumentedPresentations(List.of(
                "Adenomyosis can coexist with endometriosis and is often only diagnosed after hysterectomy.",
                "MRI has improved the ability to diagnose adenomyosis non-invasively.",
                "Symptoms are often similar to endometriosis and may be confused or co-present.",
                "Pain and bleeding can significantly impact quality of life."));
        c.setCommonlyMistakenFor(List.of("Endometriosis", "Uterine fibroids", "Normal heavy periods"));
        c.setAvgDiagnosisDelay("Often delayed; historically only diagnosed post-hysterectomy. MRI improving earlier detection. (NHS)");
        c.setSuggestedTests(List.of("Transvaginal ultrasound", "MRI of the pelvis"));
        c.setTimeframeAdvice(
                "If heavy or painful periods are affecting your quality of life, this is worth discussing with a GP. " +
                "Symptoms that do not improve with standard pain relief should be investigated further.");
        c.setCategory("reproductive");
        c.setExtraCategories(Set.of("pain"));
        c.setLifeStages(Set.of("reproductive-age", "perimenopause"));
        c.setSpecialistType("Gynaecologist");
        c.setSpecialistNotes("A gynaecologist can assess for adenomyosis using ultrasound or MRI.");
        c.setEducationalGuidance(
                "Adenomyosis is a recognised condition. Heavy or painful periods should always be discussed with a doctor.");
        c.setClinicianQuestions(List.of(
                "Could my heavy and painful periods be related to adenomyosis?",
                "Would an ultrasound or MRI help to investigate?",
                "Are there options to manage heavy bleeding or pain?",
                "Could adenomyosis and endometriosis coexist in my case?"));
        c.setSources(Set.of(sources.get("adenomyosis-nhs")));
        conditionRepo.save(c);

        addMapping(c, "heavy-periods", 0.90, "Heavy periods are one of the most documented symptoms of adenomyosis. (NHS)");
        addMapping(c, "painful-periods", 0.85, "Painful periods are a hallmark of adenomyosis. (NHS)");
        addMapping(c, "pelvic-pain", 0.80, "Pelvic pain and pressure are documented in adenomyosis. (NHS)");
        addMapping(c, "bloating", 0.45, "Bloating and abdominal pressure may be reported. (editorial review pending)", true);
        addMapping(c, "lower-back-pain", 0.50, "Lower back pain is reported in adenomyosis. (NHS Ã¢â‚¬â€œ editorial review pending)", true);
        addMapping(c, "fatigue", 0.50, "Fatigue related to heavy blood loss is documented. (NHS)");
        addMapping(c, "pain-during-sex", 0.55, "Pain during sex is documented in adenomyosis. (NHS)");
    }

    private void seedPmdd() {
        Condition c = new Condition();
        c.setName("Premenstrual Dysphoric Disorder (PMDD)");
        c.setSlug("pmdd");
        c.setBodySystem("REPRODUCTIVE");
        c.setSummary("PMDD is a severe form of premenstrual syndrome causing significant psychological and physical symptoms in the luteal phase of the menstrual cycle.");
        c.setMedicalDescription(
                "PMDD involves severe mood and physical symptoms in the luteal phase (typically 1Ã¢â‚¬â€œ2 weeks before menstruation) " +
                "that resolve after the onset of menstruation. It is distinct from PMS by the severity and functional impact.");
        c.setConventionalPresentation(
                "Marked mood changes including depression, anxiety, irritability, and mood swings; " +
                "physical symptoms such as bloating, breast tenderness, fatigue, and sleep disturbance; " +
                "all occurring in the luteal phase and resolving post-menstruation.");
        c.setFemalePresentation(
                "PMDD is frequently dismissed as 'just PMS' or attributed to stress. " +
                "The severe, cyclical nature distinguishes it from generalised depression or anxiety. " +
                "Symptom tracking across two or more cycles is key to recognition.");
        c.setDocumentedPresentations(List.of(
                "PMDD can be confused with depression or anxiety disorders because mood symptoms are prominent.",
                "The cyclical pattern Ã¢â‚¬â€ symptoms consistently appearing before menstruation and resolving after Ã¢â‚¬â€ is key to diagnosis.",
                "PMDD is recognised in DSM-5 and ICD-11.",
                "Underlying hormonal sensitivity to normal progesterone fluctuations is a documented mechanism."));
        c.setCommonlyMistakenFor(List.of("Generalised anxiety disorder", "Clinical depression", "Borderline personality disorder", "Normal PMS"));
        setAvgDiagnosisDelay(c, "Often delayed due to symptoms being normalised or attributed to mental health conditions. (editorial review pending)", true);
        c.setSuggestedTests(List.of(
                "Prospective symptom diary across at least 2 menstrual cycles",
                "Thyroid function test (to exclude hypothyroidism)",
                "Hormone blood tests if indicated"));
        c.setTimeframeAdvice(
                "If you experience severe mood or physical symptoms that reliably appear in the 1Ã¢â‚¬â€œ2 weeks before " +
                "your period and resolve once your period starts, track this across 2 cycles and discuss with your GP.");
        c.setCategory("psychological");
        c.setExtraCategories(Set.of("hormonal", "reproductive"));
        c.setLifeStages(Set.of("reproductive-age", "teen"));
        c.setSpecialistType("Gynaecologist / Psychiatrist");
        c.setSpecialistNotes("A doctor or gynaecologist experienced in PMDD can assess and offer treatment options including SSRIs or hormonal management.");
        c.setEducationalGuidance(
                "PMDD is a recognised medical condition. Tracking symptoms across the cycle for at least 2 months " +
                "is recommended before a clinical assessment.");
        c.setClinicianQuestions(List.of(
                "Could my mood and physical symptoms that follow a monthly cycle be consistent with PMDD?",
                "Would symptom tracking be useful before my next appointment?",
                "What are the treatment options for PMDD?",
                "Could an underlying condition such as PCOS or thyroid dysfunction be contributing?"));
        c.setSources(Set.of(sources.get("pmdd-nice")));
        conditionRepo.save(c);

        addMapping(c, "mood-changes", 0.95, "Severe mood changes are a core feature of PMDD. (NICE)");
        addMapping(c, "fatigue", 0.75, "Fatigue is a documented symptom of PMDD. (NICE)");
        addMapping(c, "bloating", 0.70, "Bloating is a documented physical symptom of PMDD. (NICE)");
        addMapping(c, "sleep-disturbance", 0.65, "Sleep disturbance is documented in PMDD. (NICE)");
        addMapping(c, "painful-periods", 0.50, "Painful periods can accompany PMDD. (NICE)");
        addMapping(c, "cognitive-fog", 0.55, "Difficulty concentrating is documented in PMDD. (NICE)");
        addMapping(c, "anxiety", 0.70, "Anxiety is a recognised symptom of PMDD. (NICE)");
        addMapping(c, "nausea", 0.35, "Nausea may be reported in PMDD. (editorial review pending)", true);
    }

    private void seedHypothyroidism() {
        Condition c = new Condition();
        c.setName("Hypothyroidism (Underactive Thyroid)");
        c.setSlug("hypothyroidism");
        c.setBodySystem("HORMONAL_METABOLIC");
        c.setSummary("Hypothyroidism is a condition where the thyroid gland does not produce enough thyroid hormone, causing widespread metabolic and systemic symptoms.");
        c.setMedicalDescription(
                "Primary hypothyroidism (most commonly Hashimoto's thyroiditis, an autoimmune condition) " +
                "results in insufficient thyroid hormone production. " +
                "It is diagnosed by elevated TSH and low free T4 on blood testing.");
        c.setConventionalPresentation(
                "Tiredness, weight gain, feeling cold, constipation, depression, dry skin, and slowed movements.");
        c.setFemalePresentation(
                "Symptoms develop slowly and are often attributed to stress, ageing, or depression. " +
                "Menstrual changes and fertility difficulties may be the first sign in younger women. " +
                "Diagnosis is straightforward with a blood test, but symptoms are often normalised before testing occurs.");
        c.setDocumentedPresentations(List.of(
                "Symptoms develop gradually and may be attributed to other causes or ageing.",
                "Hypothyroidism is more common in women and increases in prevalence with age.",
                "Subclinical hypothyroidism (raised TSH with normal T4) may cause symptoms in some people.",
                "Menstrual irregularities and fertility difficulties can be associated with hypothyroidism."));
        c.setCommonlyMistakenFor(List.of("Depression", "Chronic fatigue syndrome", "Fibromyalgia", "PCOS", "Anaemia"));
        c.setAvgDiagnosisDelay("Variable; symptoms often normalised for years before testing. Diagnosis straightforward once tested. (NHS)");
        c.setSuggestedTests(List.of(
                "TSH (thyroid stimulating hormone) blood test",
                "Free T4 blood test",
                "Thyroid peroxidase antibodies (for Hashimoto's)"));
        c.setTimeframeAdvice(
                "If you have had persistent fatigue, weight gain, cold intolerance, or mood changes for more " +
                "than a few weeks, ask your GP for a thyroid function blood test. This is a simple, routine test.");
        c.setCategory("hormonal");
        c.setExtraCategories(Set.of("metabolic", "autoimmune"));
        c.setLifeStages(Set.of("reproductive-age", "perimenopause", "postmenopause", "adult"));
        c.setSpecialistType("Endocrinologist / GP");
        c.setSpecialistNotes("A GP can test thyroid function with a blood test. An endocrinologist may be involved for complex cases.");
        c.setEducationalGuidance(
                "Hypothyroidism is diagnosed with a blood test for TSH (and T4). " +
                "It is treatable with thyroid hormone replacement. Ask your GP about thyroid function tests.");
        c.setClinicianQuestions(List.of(
                "Could my symptoms be related to my thyroid?",
                "Could I have a thyroid function test (TSH and free T4)?",
                "Are there any other blood tests that might be relevant?",
                "If my thyroid is underactive, what are the treatment options?",
                "Could Hashimoto's (autoimmune thyroiditis) be the cause?"));
        c.setSources(Set.of(sources.get("thyroid-nhs")));
        conditionRepo.save(c);

        addMapping(c, "fatigue", 0.90, "Tiredness/fatigue is one of the most documented symptoms of hypothyroidism. (NHS)");
        addMapping(c, "weight-gain", 0.80, "Weight gain is a well-documented symptom of hypothyroidism. (NHS)");
        addMapping(c, "cold-intolerance", 0.80, "Sensitivity to cold is a well-documented symptom of hypothyroidism. (NHS)");
        addMapping(c, "constipation", 0.70, "Constipation is documented in hypothyroidism. (NHS)");
        addMapping(c, "dry-skin", 0.70, "Dry skin and hair are documented in hypothyroidism. (NHS)");
        addMapping(c, "mood-changes", 0.65, "Depression and low mood are documented in hypothyroidism. (NHS)");
        addMapping(c, "cognitive-fog", 0.60, "Slow thinking and difficulty concentrating are documented in hypothyroidism. (NHS)");
        addMapping(c, "hair-thinning", 0.65, "Hair thinning is a documented symptom of hypothyroidism. (NHS)");
        addMapping(c, "irregular-periods", 0.50, "Menstrual irregularities can occur in hypothyroidism. (NHS)");
        addMapping(c, "sleep-disturbance", 0.45, "Sleep disturbance can occur in hypothyroidism. (editorial review pending)", true);
    }

    private void seedHeartDiseaseInWomen() {
        Condition c = new Condition();
        c.setName("Heart Disease Ã¢â‚¬â€ Atypical Presentation in Women");
        c.setSlug("heart-disease-women");
        c.setBodySystem("CARDIOVASCULAR");
        c.setSummary("Heart disease can present differently in women compared to the classic textbook description, with symptoms often being less dramatic and more easily dismissed.");
        c.setMedicalDescription(
                "Coronary artery disease and acute coronary syndromes (including heart attack) can present " +
                "atypically in women. The classic 'crushing chest pain' presentation is less common in women, " +
                "who more often experience shortness of breath, nausea, fatigue, jaw or back pain, and dizziness.");
        c.setConventionalPresentation(
                "Textbooks historically describe heart attack as sudden, severe chest pain or pressure, " +
                "radiating to the left arm, with sweating and nausea. This presentation is more common in men.");
        c.setFemalePresentation(
                "Women are more likely to experience less obvious symptoms: unusual fatigue (sometimes for " +
                "days beforehand), shortness of breath, nausea or vomiting, jaw, neck or back pain, and " +
                "dizziness. Chest discomfort may be mild or absent. These differences mean heart attacks " +
                "in women are more likely to be misdiagnosed or delayed. (AHA)");
        c.setDocumentedPresentations(List.of(
                "Women are more likely than men to have a heart attack without classic chest pain. (AHA)",
                "Symptoms such as extreme fatigue, jaw pain, or nausea may occur hours or days before a heart attack.",
                "Women with heart disease are more likely to have their symptoms attributed to anxiety or gastrointestinal issues.",
                "Microvascular coronary artery disease (smaller arteries) is more common in women and harder to detect on standard tests."));
        c.setCommonlyMistakenFor(List.of("Anxiety or panic attack", "Gastrointestinal reflux (GORD)", "Musculoskeletal pain", "Indigestion"));
        c.setAvgDiagnosisDelay("Heart disease in women is frequently delayed due to atypical presentation. (AHA)");
        c.setSuggestedTests(List.of(
                "ECG (electrocardiogram)",
                "Blood troponin levels",
                "Stress test / exercise tolerance test",
                "Coronary angiography if indicated"));
        c.setTimeframeAdvice(
                "If you experience sudden or unusual chest discomfort, shortness of breath, jaw or back pain, " +
                "or extreme unexplained fatigue Ã¢â‚¬â€ especially if these are new symptoms Ã¢â‚¬â€ seek medical attention promptly. " +
                "If symptoms are severe or sudden, call emergency services immediately.");
        c.setCategory("cardiovascular");
        c.setExtraCategories(Set.of("systemic"));
        c.setLifeStages(Set.of("perimenopause", "postmenopause", "adult"));
        c.setSpecialistType("Cardiologist");
        c.setSpecialistNotes("A cardiologist can assess heart health. An ECG and blood tests are first-line investigations.");
        c.setEducationalGuidance(
                "Heart disease is the leading cause of death in women globally. Women's symptoms differ from the " +
                "classic male presentation. If something feels very wrong, seek care Ã¢â‚¬â€ do not wait.");
        c.setClinicianQuestions(List.of(
                "Could my symptoms be consistent with a heart or cardiovascular problem?",
                "Would an ECG or blood tests be appropriate?",
                "Are there risk factors I should know about?",
                "What symptoms should prompt me to seek emergency care?",
                "Should I be referred to a cardiologist?"));
        c.setSources(Set.of(sources.get("heart-aha")));
        conditionRepo.save(c);

        addMapping(c, "chest-discomfort", 0.85, "Chest discomfort or pressure is documented in heart disease in women, though it may be milder than in men. (AHA)");
        addMapping(c, "shortness-of-breath", 0.85, "Shortness of breath is a well-documented symptom of heart attack in women. (AHA)");
        addMapping(c, "fatigue", 0.75, "Unusual or extreme fatigue is documented as a warning sign in women before a heart attack. (AHA)");
        addMapping(c, "jaw-neck-pain", 0.70, "Jaw, neck, or back pain is a documented heart attack symptom more common in women. (AHA)");
        addMapping(c, "nausea", 0.65, "Nausea is documented as a symptom of heart attack, more commonly in women. (AHA)");
        addMapping(c, "dizziness", 0.60, "Dizziness or lightheadedness is documented in women with heart attack. (AHA)");
        addMapping(c, "palpitations", 0.55, "Palpitations can occur with cardiovascular conditions. (AHA Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "anxiety", 0.40, "Anxiety and a sense of impending doom are documented with acute cardiac events. (editorial review pending)", true);
    }

    private void seedLupus() {
        Condition c = new Condition();
        c.setName("Systemic Lupus Erythematosus (Lupus / SLE)");
        c.setSlug("lupus");
        c.setBodySystem("AUTOIMMUNE");
        c.setSummary("Lupus is a chronic autoimmune condition in which the immune system attacks healthy tissue, affecting multiple body systems. It is much more common in women.");
        c.setMedicalDescription(
                "Systemic lupus erythematosus (SLE) is a multi-system autoimmune disease. " +
                "It is characterised by periods of flare and remission. " +
                "Diagnosis uses clinical criteria including the ACR/EULAR classification criteria. " +
                "It is approximately 9 times more common in women than men.");
        c.setConventionalPresentation(
                "Butterfly rash across the cheeks and nose, joint pain and swelling, fatigue, fever, " +
                "photosensitivity, and kidney involvement. Anti-dsDNA and ANA positive on blood tests.");
        c.setFemalePresentation(
                "Lupus symptoms are highly variable and fluctuate. Fatigue and joint pain are often the " +
                "predominant symptoms and may be present long before the condition is formally diagnosed. " +
                "Not everyone gets the classic butterfly rash. Symptoms are often attributed to other conditions. (NHS)");
        c.setDocumentedPresentations(List.of(
                "Lupus is known as the 'great imitator' because its symptoms overlap with many other conditions.",
                "Diagnosis can take years due to the non-specific nature of early symptoms.",
                "Flares can be triggered by sun exposure, stress, infection, or hormonal changes.",
                "Lupus is much more common in women, and in women of Black, Asian, and Hispanic heritage.",
                "Kidney involvement (lupus nephritis) is a serious complication that requires monitoring."));
        c.setCommonlyMistakenFor(List.of("Rheumatoid arthritis", "Fibromyalgia", "Chronic fatigue syndrome",
                "Multiple sclerosis", "Thyroid disease"));
        setAvgDiagnosisDelay(c, "Average 6 years from symptom onset to diagnosis in some studies. (editorial review pending)", true);
        c.setSuggestedTests(List.of(
                "ANA (antinuclear antibody) blood test",
                "Anti-dsDNA antibodies",
                "Full blood count, kidney function, urine protein",
                "Complement levels (C3, C4)"));
        c.setTimeframeAdvice(
                "If you have persistent joint pain, fatigue, and a skin rash Ã¢â‚¬â€ especially if symptoms come " +
                "and go Ã¢â‚¬â€ discuss this with your GP. Blood tests can help identify if an autoimmune condition is present.");
        c.setCategory("autoimmune");
        c.setExtraCategories(Set.of("systemic", "pain"));
        c.setLifeStages(Set.of("teen", "reproductive-age", "perimenopause"));
        c.setSpecialistType("Rheumatologist");
        c.setSpecialistNotes("A rheumatologist diagnoses and manages lupus. A GP can initiate investigations with an ANA blood test.");
        c.setEducationalGuidance(
                "Lupus is a recognised, serious medical condition that is much more common in women. " +
                "Early diagnosis and treatment can help prevent organ damage. " +
                "If you have joint pain, rash, and unexplained fatigue, ask your GP about an ANA test.");
        c.setClinicianQuestions(List.of(
                "Could my symptoms be consistent with an autoimmune condition such as lupus?",
                "Would an ANA blood test be appropriate?",
                "Are there other blood tests I should have?",
                "Should I be referred to a rheumatologist?",
                "What symptoms should prompt me to seek urgent care?"));
        c.setSources(Set.of(sources.get("lupus-nhs")));
        conditionRepo.save(c);

        addMapping(c, "fatigue", 0.85, "Fatigue is one of the most common and debilitating symptoms of lupus. (NHS)");
        addMapping(c, "joint-pain", 0.85, "Joint pain and swelling are core features of lupus. (NHS)");
        addMapping(c, "rash", 0.75, "Skin rash, including the butterfly rash, is a documented feature of lupus. (NHS)");
        addMapping(c, "widespread-pain", 0.65, "Widespread musculoskeletal pain is documented in lupus. (NHS)");
        addMapping(c, "mood-changes", 0.50, "Depression and mood changes are documented in people with lupus. (NHS Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "headache", 0.45, "Headaches are reported by some people with lupus. (NHS Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "hair-thinning", 0.55, "Hair thinning or loss is a documented feature of lupus. (NHS)");
        addMapping(c, "cognitive-fog", 0.50, "Cognitive difficulties are documented in lupus. (NHS Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "light-sensitivity", 0.60, "Photosensitivity (sensitivity to light) is a documented feature of lupus. (NHS)");
    }

    private void seedAnxietyDisorder() {
        Condition c = new Condition();
        c.setName("Generalised Anxiety Disorder (GAD)");
        c.setSlug("anxiety-disorder");
        c.setBodySystem("BRAIN_MIND");
        c.setSummary("Generalised anxiety disorder is a common mental health condition characterised by persistent, excessive worry and physical symptoms of anxiety. It is more common in women.");
        c.setMedicalDescription(
                "GAD is diagnosed when a person has persistent, uncontrollable worry about multiple topics " +
                "for at least 6 months, accompanied by physical symptoms such as muscle tension, fatigue, " +
                "and sleep disturbance. It is more prevalent in women.");
        c.setConventionalPresentation(
                "Persistent excessive worry, restlessness, irritability, muscle tension, sleep disturbance, " +
                "difficulty concentrating, and fatigue. Diagnosed using DSM-5 or ICD-11 criteria.");
        c.setFemalePresentation(
                "Women are twice as likely to be diagnosed with an anxiety disorder as men. " +
                "Physical symptoms such as muscle tension, headaches, and gastrointestinal upset are common. " +
                "Anxiety can be dismissed as 'stress' or misattributed to a physical health problem, and vice versa. (NHS)");
        c.setDocumentedPresentations(List.of(
                "Anxiety disorders are more common in women than men.",
                "Anxiety can present primarily with physical symptoms, which may delay psychological diagnosis.",
                "Cyclical worsening (e.g. premenstrually) may indicate PMDD rather than or alongside GAD.",
                "Anxiety can coexist with depression and other mental health or physical conditions."));
        c.setCommonlyMistakenFor(List.of("PMDD", "Hyperthyroidism", "Heart palpitations", "Depression", "Fibromyalgia"));
        setAvgDiagnosisDelay(c, "Average 9 years from onset to treatment in some studies. (editorial review pending)", true);
        c.setSuggestedTests(List.of(
                "GAD-7 or PHQ-9 screening questionnaire",
                "Thyroid function test (to exclude hyperthyroidism)",
                "Blood tests to exclude physical causes if indicated"));
        c.setTimeframeAdvice(
                "If excessive worry, physical tension, or sleep difficulties have been affecting your life for " +
                "more than a few weeks, discuss this with your GP. There are effective treatments available.");
        c.setCategory("psychological");
        c.setExtraCategories(Set.of("neurological"));
        c.setLifeStages(Set.of("teen", "reproductive-age", "perimenopause", "postmenopause", "adult"));
        c.setSpecialistType("GP / Psychologist / Psychiatrist");
        c.setSpecialistNotes("A GP can assess and treat anxiety. A referral to psychological therapy (e.g. CBT) is often first-line.");
        c.setEducationalGuidance(
                "Anxiety disorders are real medical conditions with effective treatments. " +
                "Physical symptoms of anxiety can be just as disabling as psychological ones. " +
                "If anxiety is affecting your daily life, ask your GP about talking therapies or other options.");
        c.setClinicianQuestions(List.of(
                "Could my symptoms be consistent with an anxiety disorder?",
                "Would a screening questionnaire such as the GAD-7 be helpful?",
                "Are there physical causes that should be excluded first?",
                "What treatment options are available Ã¢â‚¬â€ talking therapy, medication, or both?",
                "Is my anxiety possibly linked to my menstrual cycle?"));
        c.setSources(Set.of(sources.get("anxiety-nhs")));
        conditionRepo.save(c);

        addMapping(c, "anxiety", 0.95, "Persistent anxiety and worry are core features of GAD. (NHS)");
        addMapping(c, "sleep-disturbance", 0.80, "Sleep disturbance is a core feature of GAD. (NHS)");
        addMapping(c, "fatigue", 0.75, "Fatigue caused by anxiety is well-documented. (NHS)");
        addMapping(c, "cognitive-fog", 0.65, "Difficulty concentrating is a documented feature of GAD. (NHS)");
        addMapping(c, "mood-changes", 0.65, "Irritability and low mood are documented alongside anxiety. (NHS)");
        addMapping(c, "headache", 0.55, "Tension headaches are commonly associated with anxiety. (NHS Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "palpitations", 0.60, "Heart palpitations are a documented physical symptom of anxiety. (NHS)");
        addMapping(c, "dizziness", 0.50, "Dizziness can be a physical symptom of anxiety. (NHS Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "nausea", 0.50, "Nausea is a documented physical symptom of anxiety. (NHS)");
    }

    private void seedMenstrualMigraine() {
        Condition c = new Condition();
        c.setName("Menstrual Migraine");
        c.setSlug("menstrual-migraine");
        c.setBodySystem("BRAIN_MIND");
        c.setSummary("Menstrual migraine is a pattern of migraine attacks that occur in association with the menstrual cycle, often related to hormonal changes around menstruation.");
        c.setMedicalDescription(
                "Menstrual migraine refers to migraine attacks that reliably occur in the perimenstrual window " +
                "(2 days before to 3 days after the start of menstruation). " +
                "It is thought to be triggered by the drop in oestrogen levels before menstruation. " +
                "It is more often without aura than other migraines.");
        c.setConventionalPresentation(
                "Severe, often one-sided headache with pulsating quality, nausea, vomiting, " +
                "and sensitivity to light and sound, occurring around the time of menstruation.");
        c.setFemalePresentation(
                "Menstrual migraines are often more severe and longer-lasting than non-menstrual migraines, " +
                "and are more resistant to treatment. The hormonal link means cycle tracking is important " +
                "for identifying the pattern and discussing hormonal management options. (NHS)");
        c.setDocumentedPresentations(List.of(
                "Menstrual migraines are more severe and longer than migraines at other times of the cycle.",
                "They are often without aura, unlike some other migraine types.",
                "The relationship to the cycle can be masked by irregular periods.",
                "Hormonal contraception or HRT can influence menstrual migraine patterns."));
        c.setCommonlyMistakenFor(List.of("Tension headache", "PMS headache", "Non-menstrual migraine", "PMDD"));
        c.setAvgDiagnosisDelay("Often not formally identified as menstrual-linked without cycle tracking. (NHS)");
        c.setSuggestedTests(List.of(
                "Headache and menstrual cycle diary (at least 3 months)",
                "Neurological assessment if headache features are atypical"));
        c.setTimeframeAdvice(
                "If you regularly have severe headaches that coincide with your period, track this over 2Ã¢â‚¬â€œ3 " +
                "cycles and discuss with your GP. Effective treatments exist, including hormonal approaches.");
        c.setCategory("neurological");
        c.setExtraCategories(Set.of("hormonal", "pain"));
        c.setLifeStages(Set.of("teen", "reproductive-age", "perimenopause"));
        c.setSpecialistType("GP / Neurologist");
        c.setSpecialistNotes("A GP can diagnose menstrual migraine and initiate treatment. A neurologist may be involved for complex cases.");
        c.setEducationalGuidance(
                "Menstrual migraine is a recognised condition linked to hormonal changes. " +
                "Tracking your headaches alongside your cycle is the key first step.");
        c.setClinicianQuestions(List.of(
                "Could my headaches be related to my menstrual cycle?",
                "Would cycle-linked migraine treatment strategies be appropriate for me?",
                "Are hormonal approaches to prevention suitable given my situation?",
                "What acute treatments are recommended for menstrual migraine?",
                "Are there any red-flag headache features I should watch for?"));
        c.setSources(Set.of(sources.get("migraine-nhs")));
        conditionRepo.save(c);

        addMapping(c, "headache", 0.95, "Severe headache is the defining feature of migraine. (NHS)");
        addMapping(c, "nausea", 0.80, "Nausea and vomiting are core features of migraine. (NHS)");
        addMapping(c, "light-sensitivity", 0.80, "Sensitivity to light and sound is a core feature of migraine. (NHS)");
        addMapping(c, "fatigue", 0.65, "Fatigue before, during, and after migraine is well-documented. (NHS)");
        addMapping(c, "cognitive-fog", 0.60, "Cognitive difficulties ('brain fog') are documented in the migraine prodrome and postdrome. (NHS)");
        addMapping(c, "dizziness", 0.55, "Dizziness can accompany migraine attacks. (NHS Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "mood-changes", 0.50, "Mood changes are documented in the migraine prodrome phase. (NHS Ã¢â‚¬â€ editorial review pending)", true);
        addMapping(c, "painful-periods", 0.45, "Painful periods co-occurring with headaches can suggest menstrual migraine. (NHS)");
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // HELPERS Ã¢â‚¬â€œ condition-symptom mapping
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /** Adds a mapping with no editorial review flag (i.e. evidence is considered adequate). */
    private void addMapping(Condition condition, String symptomSlug,
                             double weight, String evidenceNote) {
        addMapping(condition, symptomSlug, weight, evidenceNote, false);
    }

    private void addMapping(Condition condition, String symptomSlug,
                             double weight, String evidenceNote,
                             boolean pendingReview) {
        Symptom symptom = symptoms.get(symptomSlug);
        if (symptom == null) {
            log.warn("DataLoader: symptom slug '{}' not found, skipping mapping for '{}'",
                    symptomSlug, condition.getSlug());
            return;
        }
        String note = pendingReview ? "[NEEDS EDITORIAL REVIEW] " + evidenceNote : evidenceNote;
        ConditionSymptom cs = new ConditionSymptom();
        cs.setCondition(condition);
        cs.setSymptom(symptom);
        cs.setOverlapWeight(weight);
        cs.setEvidenceNote(note);
        csRepo.save(cs);
    }

    // helper used for avgDiagnosisDelay when pendingReview=true
    private void setAvgDiagnosisDelay(Condition c, String delay, boolean pendingReview) {
        c.setAvgDiagnosisDelay(pendingReview ? "[NEEDS EDITORIAL REVIEW] " + delay : delay);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    // WARNING SIGNS
    // Sourced only from authoritative bodies. Do not add invented statistics
    // or unsourced clinical guidance.
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private void seedWarningSigns() {
        // Heavy bleeding
        saveWarningSignIfAbsent("Very heavy menstrual bleeding",
                "Soaking through a pad or tampon every hour for several consecutive hours can indicate " +
                "significant blood loss that requires medical attention.",
                "Seek urgent medical care if you are soaking through a pad or tampon hourly for two or more hours.",
                List.of("heavy-periods"),
                List.of("adenomyosis", "endometriosis"),
                "https://www.nhs.uk/conditions/heavy-periods/",
                "Heavy periods Ã¢â‚¬â€œ NHS",
                1, null);

        // Severe sudden pelvic pain
        saveWarningSignIfAbsent("Sudden severe pelvic or abdominal pain",
                "Sudden, severe pelvic or abdominal pain can indicate a serious condition such as " +
                "an ectopic pregnancy, ovarian torsion, or appendicitis, all of which require emergency care.",
                "Call emergency services (999/112) or go to A&E immediately for sudden severe pelvic or abdominal pain.",
                List.of("pelvic-pain"),
                List.of("endometriosis", "adenomyosis"),
                "https://www.nhs.uk/conditions/stomach-ache/",
                "Stomach ache and abdominal pain Ã¢â‚¬â€œ NHS",
                1, null);

        // Urinary blood
        saveWarningSignIfAbsent("Blood in urine",
                "Blood in the urine (haematuria) should always be assessed by a doctor to rule out " +
                "serious underlying causes including bladder or kidney conditions.",
                "Contact your GP promptly if you notice blood in your urine.",
                List.of("urinary-symptoms"),
                List.of(),
                "https://www.nhs.uk/conditions/blood-in-urine/",
                "Blood in urine (haematuria) Ã¢â‚¬â€œ NHS",
                1, null);

        // Heart attack warning Ã¢â‚¬â€ requires 3+ cardiac symptoms together
        saveWarningSignIfAbsent("Possible cardiac emergency signs",
                "A combination of chest discomfort, shortness of breath, jaw or back pain, nausea, " +
                "or extreme fatigue Ã¢â‚¬â€ especially if sudden or new Ã¢â‚¬â€ can indicate a cardiac emergency. " +
                "Women's heart attack symptoms are often different from the classic description and can be subtle.",
                "Call emergency services (999/112) immediately if you have a combination of these symptoms and they are new or sudden.",
                List.of("chest-discomfort", "shortness-of-breath", "jaw-neck-pain", "nausea", "dizziness", "palpitations"),
                List.of("heart-disease-women"),
                "https://www.heart.org/en/health-topics/heart-attack/warning-signs-of-a-heart-attack/heart-attack-symptoms-in-women",
                "Heart Attack Symptoms in Women Ã¢â‚¬â€œ American Heart Association",
                3, null);

        // Lupus red flag Ã¢â‚¬â€ new or worsening kidney symptoms
        saveWarningSignIfAbsent("Possible kidney involvement in autoimmune condition",
                "Foamy urine, swelling in the legs, ankles, or feet, and elevated blood pressure alongside " +
                "autoimmune symptoms may suggest kidney involvement, which requires prompt medical assessment.",
                "Contact your doctor promptly if you have autoimmune symptoms alongside urinary changes or significant swelling.",
                List.of("urinary-symptoms", "rash", "joint-pain"),
                List.of("lupus"),
                "https://www.nhs.uk/conditions/lupus/",
                "Lupus Ã¢â‚¬â€œ NHS",
                2, null);

        // Stroke warning signs Ã¢â‚¬â€ FAST (Face, Arms, Speech, Time)
        saveWarningSignIfAbsent("Possible stroke warning signs (FAST)",
                "Sudden face drooping on one side, arm weakness or numbness, slurred or strange speech, " +
                "or a sudden severe headache with no known cause can be signs of a stroke or TIA. " +
                "Use the FAST test: Face, Arms, Speech, Time to call 999.",
                "Call emergency services (999/112) immediately. Time is critical Ã¢â‚¬â€ do not wait to see if symptoms improve.",
                List.of("dizziness", "headache", "cognitive-fog"),
                List.of(),
                "https://www.nhs.uk/conditions/stroke/symptoms/",
                "Stroke symptoms Ã¢â‚¬â€œ NHS",
                2, null);

        // Postpartum warning signs
        saveWarningSignIfAbsent("Postpartum warning signs",
                "After giving birth, symptoms such as heavy bleeding soaking more than one pad per hour, " +
                "severe headache, vision changes, chest pain, or difficulty breathing can indicate serious " +
                "postpartum complications including haemorrhage, pre-eclampsia, or pulmonary embolism.",
                "Seek emergency care immediately for heavy bleeding, severe headache, vision changes, chest pain, or difficulty breathing after giving birth.",
                List.of("heavy-periods", "headache", "shortness-of-breath", "chest-discomfort"),
                List.of(),
                "https://www.nhs.uk/pregnancy/labour-and-birth/after-the-birth/postpartum-complications/",
                "Postpartum complications Ã¢â‚¬â€œ NHS",
                2, null);
    }

    /**
     * Saves a warning sign only if one with the same name does not already exist.
     * Safe to call on every startup Ã¢â‚¬â€ idempotent.
     */
    // -------------------------------------------------------------------------
    // INCREMENTAL ALIAS UPDATES
    // Called on every startup to add new aliases to existing symptom records.
    // Uses addAliasIfAbsent() which is idempotent: it skips aliases that are
    // already present, so repeated runs are safe on a live database.
    // -------------------------------------------------------------------------

    private void incrementalAliasUpdates() {
        // Periods
        addAliasIfAbsent("painful-periods", "bad cramps");
        addAliasIfAbsent("painful-periods", "bad period pain");
        addAliasIfAbsent("painful-periods", "severe cramps");
        addAliasIfAbsent("painful-periods", "terrible cramps");

        addAliasIfAbsent("irregular-periods", "late period");
        addAliasIfAbsent("irregular-periods", "late periods");
        addAliasIfAbsent("irregular-periods", "spotting");
        addAliasIfAbsent("irregular-periods", "light bleeding");
        addAliasIfAbsent("irregular-periods", "irregular bleeding");

        addAliasIfAbsent("heavy-periods", "very heavy periods");
        addAliasIfAbsent("heavy-periods", "soaking through pads");

        // Pelvic and abdominal
        addAliasIfAbsent("pelvic-pain", "lower belly pain");
        addAliasIfAbsent("pelvic-pain", "belly pain");
        addAliasIfAbsent("pelvic-pain", "tummy ache");
        addAliasIfAbsent("pelvic-pain", "lower tummy pain");

        // Fatigue and general
        addAliasIfAbsent("fatigue", "tired all the time");
        addAliasIfAbsent("fatigue", "always exhausted");
        addAliasIfAbsent("fatigue", "weakness");
        addAliasIfAbsent("fatigue", "feeling weak");
        addAliasIfAbsent("fatigue", "no energy at all");

        // Neurological
        addAliasIfAbsent("dizziness", "feeling dizzy");
        addAliasIfAbsent("dizziness", "dizzy spells");
        addAliasIfAbsent("dizziness", "dizzy");

        addAliasIfAbsent("cognitive-fog", "trouble concentrating");
        addAliasIfAbsent("cognitive-fog", "can't concentrate");
        addAliasIfAbsent("cognitive-fog", "mind fog");
        addAliasIfAbsent("cognitive-fog", "foggy head");

        addAliasIfAbsent("headache", "headaches");
        addAliasIfAbsent("headache", "bad headache");

        // Mood and mental health
        addAliasIfAbsent("mood-changes", "feeling low");
        addAliasIfAbsent("mood-changes", "sad");
        addAliasIfAbsent("mood-changes", "emotional");
        addAliasIfAbsent("mood-changes", "crying a lot");
        addAliasIfAbsent("mood-changes", "mood changes");

        addAliasIfAbsent("sleep-disturbance", "trouble sleeping");
        addAliasIfAbsent("sleep-disturbance", "difficulty sleeping");
        addAliasIfAbsent("sleep-disturbance", "sleepless");
        addAliasIfAbsent("sleep-disturbance", "waking up tired");

        addAliasIfAbsent("anxiety", "feeling anxious");
        addAliasIfAbsent("anxiety", "constant worry");
        addAliasIfAbsent("anxiety", "on edge");

        // Urinary
        addAliasIfAbsent("urinary-symptoms", "burning while peeing");
        addAliasIfAbsent("urinary-symptoms", "burning urination");
        addAliasIfAbsent("urinary-symptoms", "painful peeing");
        addAliasIfAbsent("urinary-symptoms", "peeing a lot");
        addAliasIfAbsent("urinary-symptoms", "needing to pee urgently");
        addAliasIfAbsent("urinary-symptoms", "urinary burning");

        // Digestive
        addAliasIfAbsent("nausea", "vomiting");
        addAliasIfAbsent("nausea", "feeling nauseous");
        addAliasIfAbsent("nausea", "throwing up");
        addAliasIfAbsent("nausea", "being sick");

        addAliasIfAbsent("constipation", "can't go to the toilet");
        addAliasIfAbsent("constipation", "not going to the toilet");
        addAliasIfAbsent("constipation", "straining");
    }

    /**
     * Adds {@code alias} to the symptom identified by {@code slug} if it is not
     * already present. Safe to run on an existing database; skipped if the
     * symptom does not exist or already has the alias.
     */
    private void addAliasIfAbsent(String slug, String alias) {
        symptomRepo.findBySlug(slug).ifPresent(s -> {
            if (s.getAliases() == null) {
                s.setAliases(new ArrayList<>());
            }
            String lc = alias.toLowerCase(java.util.Locale.ROOT);
            boolean already = s.getAliases().stream()
                    .anyMatch(a -> a.toLowerCase(java.util.Locale.ROOT).equals(lc));
            if (!already) {
                s.getAliases().add(alias);
                symptomRepo.save(s);
                log.debug("DataLoader: added alias '{}' to symptom '{}'", alias, slug);
            }
        });
    }

    private void saveWarningSignIfAbsent(String name, String message, String action,
                                          List<String> triggerSlugs, List<String> conditionSlugs,
                                          String sourceUrl, String sourceTitle,
                                          int minMatches, String requiredLifeStage) {
        if (warningSignRepo.findByName(name).isPresent()) {
            log.debug("DataLoader: warning sign '{}' already exists, skipping.", name);
            return;
        }
        WarningSign w = new WarningSign();
        w.setName(name);
        w.setMessage(message);
        w.setRecommendedAction(action);
        w.setTriggerSymptomSlugs(new ArrayList<>(triggerSlugs));
        w.setRelatedConditionSlugs(new ArrayList<>(conditionSlugs));
        w.setSourceUrl(sourceUrl);
        w.setSourceTitle(sourceTitle);
        w.setMinMatches(minMatches);
        w.setRequiredLifeStage(requiredLifeStage);
        warningSignRepo.save(w);
        log.info("DataLoader: added warning sign '{}'", name);
    }
}
