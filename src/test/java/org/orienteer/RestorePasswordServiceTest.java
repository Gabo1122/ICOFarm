package org.orienteer;

import com.google.inject.Inject;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.security.OSecurityUser;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.schedule.OScheduledEvent;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.orienteer.junit.OrienteerTestRunner;
import org.orienteer.model.ICOFarmUser;
import org.orienteer.service.IRestorePasswordService;
import ru.ydn.wicket.wicketorientdb.utils.DBClosure;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(OrienteerTestRunner.class)
public class RestorePasswordServiceTest {

    @Inject
    private IRestorePasswordService service;

    private OProperty property;
    private String cronValue;
    private String timeoutValue;
    private ICOFarmUser user;

    @Before
    public void init() {
        DBClosure.sudoConsumer(db -> {
            user = new ICOFarmUser(new ODocument(OUser.CLASS_NAME));
            user.setEmail(UUID.randomUUID().toString() + "@gmail.com")
                    .setId(UUID.randomUUID().toString());
            user.setName(UUID.randomUUID().toString())
                    .setPassword(UUID.randomUUID().toString())
                    .setAccountStatus(OSecurityUser.STATUSES.ACTIVE);
            property = user.getDocument().getSchemaClass().getProperty(ICOFarmUser.OPROPERTY_RESTORE_ID);
            cronValue = ICOFarmApplication.REMOVE_CRON_RULE.getValue(property);
            timeoutValue = ICOFarmApplication.REMOVE_SCHEDULE_START_TIMEOUT.getValue(property);
            ICOFarmApplication.REMOVE_CRON_RULE.setValue(property, "0 0/1 * 1/1 * ? *");
            ICOFarmApplication.REMOVE_SCHEDULE_START_TIMEOUT.setValue(property, "3000");
            user.save();
        });
    }

    @After
    public void destroy() {
       DBClosure.sudoConsumer(db -> {
           db.command(new OCommandSQL("DELETE FROM ?")).execute(user.getDocument());
           ICOFarmApplication.REMOVE_CRON_RULE.setValue(property, cronValue);
           ICOFarmApplication.REMOVE_SCHEDULE_START_TIMEOUT.setValue(property, timeoutValue);
       });
    }

    @Test
    public void testService() throws InterruptedException {
        service.restoreUserPassword(user);
        assertNotNull(getRestoreSchedulerEvent(user.getRestoreId()));
        Thread.currentThread().join(70_000);
        assertNull(getRestoreSchedulerEvent(user.getRestoreId()));
    }

    private OScheduledEvent getRestoreSchedulerEvent(String id) {
        return DBClosure.sudo(db -> db.getMetadata().getScheduler().getEvent("removeUserRestoreId" + id));
    }
}
