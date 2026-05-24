package com.example.backend.Service;

import com.example.backend.Model.Client;
import com.example.backend.Model.Parameter;
import com.example.backend.Repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientAgeingService {

    private final ClientRepository clientRepository;
    private final ParameterService parameterService;

    // تخدم كل ليلة في نصف الليل (00:00) حسب الـ Cron Expression
    @Scheduled(cron = "0 */2 * * * ?")
    @Transactional
    public void updateDailyClientAgeing() {
        log.info("▶️ بداية التحديث اليومي لأيام التأخير والـ Structures...");

        // 1. جلب البارامترات م الداتابيز
        Parameter amiableIMP = parameterService.getByCodeAndType("phase amiable", "IMP");
        Parameter amiableSDB = parameterService.getByCodeAndType("phase amiable", "SDB");
        Parameter commIMP = parameterService.getByCodeAndType("phase commerciale", "IMP");
        Parameter commSDB = parameterService.getByCodeAndType("phase commerciale", "SDB");

        // 2. جلب كافه العملاء (أو فقط العملاء اللي مش Clôturé)
        List<Client> clients = clientRepository.findAll();

        for (Client client : clients) {

            // المنطق: إذا العميل خلص (المبالغ 0)، نرجع الأيام 0
            if (client.getTotalImpayeAmount().signum() <= 0 && client.getTotalSdbAmount().signum() <= 0) {
                client.setTotalDaysImpaye(0L);
                client.setTotalDaysSdb(0L);
                // تنجم هوني ترجع الـ Structure الأصلية متاعو أو تخليه
                continue;
            }

            // 3. زيادة نهار كل يوم لو فما مبالغ غير مدفوعة
            if (client.getTotalImpayeAmount().signum() > 0) {
                client.setTotalDaysImpaye(client.getTotalDaysImpaye() + 1);
            }
            if (client.getTotalSdbAmount().signum() > 0) {
                client.setTotalDaysSdb(client.getTotalDaysSdb() + 1);
            }

            // 4. تطبيق نفس الـ Logic متاع الـ Structure
            long daysSdb = client.getTotalDaysSdb();
            long daysImpaye = client.getTotalDaysImpaye();

            if (daysSdb > 0) {
                if (checkRange(daysSdb, amiableSDB)) client.setStructure("S003");
                else if (checkRange(daysSdb, commSDB)) client.setStructure("S002");
            } else {
                if (checkRange(daysImpaye, amiableIMP)) client.setStructure("S003");
                else if (checkRange(daysImpaye, commIMP)) client.setStructure("S002");
            }
        }

        // 5. حفظ التغييرات الكل فرد مرة (Batch Update)
        clientRepository.saveAll(clients);
        log.info("✅ تم تحديث الـ Structures بنجاح لـ {} عميل.", clients.size());
    }

    private boolean checkRange(long days, Parameter param) {
        return param != null && days >= param.getJourDebut() && days <= param.getJourFin();
    }
}