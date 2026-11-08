package com.queueless.config;

import com.queueless.entity.Business;
import com.queueless.entity.Queue;
import com.queueless.entity.ServiceEntity;
import com.queueless.entity.User;
import com.queueless.enums.QueueStatus;
import com.queueless.enums.UserRole;
import com.queueless.repository.BusinessRepository;
import com.queueless.repository.QueueRepository;
import com.queueless.repository.ServiceRepository;
import com.queueless.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Automatically seeds the default persistent Apex Diagnostic Clinic and its 3 service queues
 * once on application startup. This ensures every connected tab and mobile device joins the
 * same active queues.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final QueueRepository queueRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           BusinessRepository businessRepository,
                           ServiceRepository serviceRepository,
                           QueueRepository queueRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.serviceRepository = serviceRepository;
        this.queueRepository = queueRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<Queue> openQueues = queueRepository.findByStatus(QueueStatus.OPEN);
        if (!openQueues.isEmpty()) {
            log.info("Active clinic queues already exist (Count = {}). Skipping initialization.", openQueues.size());
            return;
        }

        log.info("No active queues found. Initializing persistent clinic and queues...");

        // 1. Create or retrieve Staff/Owner User
        User owner = userRepository.findByEmail("owner@apexclinic.com")
                .orElseGet(() -> userRepository.save(
                        new User("Dr. Sarah Lin (Chief of Staff)",
                                "owner@apexclinic.com",
                                passwordEncoder.encode("Password123!"),
                                UserRole.BUSINESS_OWNER)
                ));

        // 2. Create or retrieve Default Customer User
        userRepository.findByEmail("alex@example.com")
                .orElseGet(() -> userRepository.save(
                        new User("Alex Johnson",
                                "alex@example.com",
                                passwordEncoder.encode("Password123!"),
                                UserRole.CUSTOMER)
                ));

        // 3. Create Clinic Business
        Business clinic = businessRepository.save(new Business(
                "Apex Diagnostic Imaging Clinic",
                "100 Medical Center Blvd, Suite 400",
                "555-432-1098",
                "Advanced multi-specialty and diagnostic clinic",
                owner
        ));

        // 4. Create Clinic Services
        ServiceEntity s1 = serviceRepository.save(new ServiceEntity(
                "General Consultation & Triage",
                "Primary health consultation, vitals, and physician checkup",
                15,
                clinic
        ));

        ServiceEntity s2 = serviceRepository.save(new ServiceEntity(
                "MRI Scan & Diagnostic Review",
                "Comprehensive 50-point diagnostic scan and radiology",
                30,
                clinic
        ));

        ServiceEntity s3 = serviceRepository.save(new ServiceEntity(
                "Blood Test & Routine Lab Work",
                "Fast blood draw, metabolic panel, and lab sample intake",
                10,
                clinic
        ));

        // 5. Open Queues for Today
        LocalDate today = LocalDate.now();
        Queue q1 = queueRepository.save(new Queue(s1, today, 2));
        Queue q2 = queueRepository.save(new Queue(s2, today, 2));
        Queue q3 = queueRepository.save(new Queue(s3, today, 1));

        log.info("Persistent clinic initialized successfully! Open Queues: #{} (General), #{} (MRI), #{} (Blood Test)",
                q1.getId(), q2.getId(), q3.getId());
    }
}

