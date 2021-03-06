package com.bionic.service.impl;

import com.bionic.dao.ShiftDao;
import com.bionic.dto.WorkingWeekDTO;
import com.bionic.exception.shift.impl.ShiftsFromFuturePeriodException;
import com.bionic.exception.shift.impl.ShiftsNotFoundException;
import com.bionic.model.Ride;
import com.bionic.model.Shift;
import com.bionic.service.OvertimeService;
import com.bionic.service.SummaryService;
import com.bionic.service.WorkScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

import static com.bionic.service.util.MonthCalculator.getMonthEndTime;
import static com.bionic.service.util.MonthCalculator.getMonthStartTime;
import static com.bionic.service.util.PeriodCalculator.*;
import static com.bionic.service.util.WeekCalculator.*;

/**
 * @author Pavel Boiko
 */
@Service
public class SummaryServiceImpl implements SummaryService {

    @Autowired
    private ShiftDao shiftDao;

    @Autowired
    private OvertimeService overtimeService;

    @Autowired
    private WorkScheduleService workScheduleService;

    @Override
    public WorkingWeekDTO getSummaryTotal(List<WorkingWeekDTO> workingWeekDtoList) {
        WorkingWeekDTO workingWeekDTO = new WorkingWeekDTO();

        for (WorkingWeekDTO w : workingWeekDtoList) {
            workingWeekDTO.setWorkedTime(workingWeekDTO.getWorkedTime() + w.getWorkedTime());
            System.out.println("SUMMARY WEEK = " + w.getWorkedTime());
            System.out.println("SUMMARY TOTAL = " + workingWeekDTO.getWorkedTime() / MILLIS_IN_HOUR);
        }

        return workingWeekDTO;
    }

    public List<WorkingWeekDTO> getSummaryForMonth(int userId, int year, int month)
                                    throws ShiftsNotFoundException, ShiftsFromFuturePeriodException {

        Date monthStartTime = getMonthStartTime(year, month);
        Date monthEndTime = getMonthEndTime(year, month);
        Date currentTime = Calendar.getInstance().getTime();

        if (monthStartTime.after(currentTime)) throw new ShiftsFromFuturePeriodException();

        List<Shift> shifts = shiftDao.getForPeriod(userId, monthStartTime, monthEndTime);
        if (ObjectUtils.isEmpty(shifts)) throw new ShiftsNotFoundException();

        List<WorkingWeekDTO> summary = new ArrayList<>();
        int numberOfWeeks = getWeeksBetween(monthStartTime, monthEndTime);

        for (int week = 1; week <= numberOfWeeks; week++) {
            Date weekStartTime = getMonthWeekStartTime(year, month, week);
            Date weekEndTime = getMonthWeekEndTime(year, month, week);
            Date workingWeekEndTime = getWorkingWeekEndTime(weekEndTime);
            Date saturdayStartTime = getSaturdayStartTime(weekStartTime);
            Date saturdayEndTime = getSaturdayEndTime(weekEndTime);
            Date sundayStartTime = getSundayStartTime(weekStartTime);

            int contractHours = 0;
            contractHours = workScheduleService.getContractHoursForWeek(userId, weekStartTime);
            if (contractHours == 0) contractHours = 40;
            long contractTime = contractHours * 60 * 60 * 1000;
            int weekNumber = getMonthWeekOfYear(year, month, week);

            WorkingWeekDTO workingWeek = getSummaryForWeek(shifts, weekStartTime, weekEndTime, contractTime);

            long actualWorkedTime = overtimeService.getWorkedTimeForPeriod(shifts, weekStartTime, workingWeekEndTime);
            long overTime = 0;
            if (actualWorkedTime > contractTime) {
                overTime = actualWorkedTime - contractTime;
            }
            long saturdayWorkedTime = overtimeService.getWorkedTimeForPeriod(shifts, saturdayStartTime, saturdayEndTime);
            long sundayWorkedTime = overtimeService.getWorkedTimeForPeriod(shifts, sundayStartTime, weekEndTime);
            long totalTime = actualWorkedTime + saturdayWorkedTime + sundayWorkedTime;
            workingWeek.setWeekNumber(weekNumber);
            workingWeek.setWorkedTime(totalTime);
            workingWeek.setOverTime(overTime);
            System.out.println("worked time for week " + weekNumber + " = " + (workingWeek.getWorkedTime() / 1000 / 60 / 60d));
            summary.add(workingWeek);
        }

        return summary;
    }

    public List<WorkingWeekDTO> getSummaryForPeriod(int userId, int year, int period)
                                    throws ShiftsNotFoundException, ShiftsFromFuturePeriodException {

        Date periodStartTime = getPeriodStartTime(year, period);
        Date periodEndTime = getPeriodEndTime(year, period);
        Date currentTime = Calendar.getInstance().getTime();

        if (periodStartTime.after(currentTime)) throw new ShiftsFromFuturePeriodException();

        List<Shift> shifts = shiftDao.getForPeriod(userId, periodStartTime, periodEndTime);
        if (ObjectUtils.isEmpty(shifts)) throw new ShiftsNotFoundException();

        List<WorkingWeekDTO> summary = new ArrayList<>();

        for (int week = 1; week <= NUMBER_OF_WEEKS_IN_PERIOD; week++) {
            Date weekStartTime = getPeriodWeekStartTime(year, period, week);
            Date weekEndTime = getPeriodWeekEndTime(year, period, week);
            Date workingWeekEndTime = getWorkingWeekEndTime(weekEndTime);
            Date saturdayStartTime = getSaturdayStartTime(weekStartTime);
            Date saturdayEndTime = getSaturdayEndTime(weekEndTime);
            Date sundayStartTime = getSundayStartTime(weekStartTime);
//            System.out.println("weekStartTime = " + weekStartTime);
//            System.out.println("workingWeekEndTime = " + workingWeekEndTime);
//            System.out.println("saturdayStartTime = " + saturdayStartTime);
//            System.out.println("saturdayEndTime = " + saturdayEndTime);
//            System.out.println("sundayStartTime = " + sundayStartTime);
//            System.out.println("weekEndTime = " + weekEndTime);

            int contractHours = 0;
            contractHours = workScheduleService.getContractHoursForWeek(userId, weekStartTime);
            if (contractHours == 0) contractHours = 40;
            long contractTime = contractHours * 60 * 60 * 1000;
            int weekNumber = getPeriodWeekOfYear(year, period, week);

            WorkingWeekDTO workingWeek = getSummaryForWeek(shifts, weekStartTime, weekEndTime, contractTime);

            //Override previous calculations by OvertimeService data
            System.out.println("time for working week");
            long actualWorkedTime = overtimeService.getWorkedTimeForPeriod(shifts, weekStartTime, workingWeekEndTime);
            long overTime = 0;
            if (actualWorkedTime > contractTime) {
                overTime = actualWorkedTime - contractTime;
            }
            System.out.println("saturday worked time");
            long saturdayWorkedTime = overtimeService.getWorkedTimeForPeriod(shifts, saturdayStartTime, saturdayEndTime);
            System.out.println("sunday worked time");
            long sundayWorkedTime = overtimeService.getWorkedTimeForPeriod(shifts, sundayStartTime, weekEndTime);
            long totalTime = actualWorkedTime + saturdayWorkedTime + sundayWorkedTime;

            workingWeek.setWeekNumber(weekNumber);
            workingWeek.setWorkedTime(totalTime);
            workingWeek.setOverTime(overTime);
            System.out.println("worked time for week " + weekNumber + " = " + (workingWeek.getWorkedTime() / 1000 / 60 / 60d));
            summary.add(workingWeek);
        }

        return summary;
    }

    public WorkingWeekDTO getSummaryForWeek(List<Shift> shifts, Date weekStartTime, Date weekEndTime, long contractTime) {

        WorkingWeekDTO workingWeek = new WorkingWeekDTO();
        Set<Shift> shiftSet = new HashSet<>();
        int workedTime = 0;
        int pauseTime = 0;
        Collections.sort(shifts, (l, r) -> (int)(l.getStartTime().getTime() - r.getStartTime().getTime()));

        shift:
        for (Shift s : shifts) {

            List<Ride> rides = s.getRides();
            Collections.sort(rides, (l, r) -> (int)(l.getStartTime().getTime() - r.getStartTime().getTime()));

            for (int i = 0; i < rides.size(); i++) {
                Ride r = rides.get(i);
                if (r.getEndTime().getTime() > weekStartTime.getTime()) {
                    if (r.getStartTime().getTime() > weekEndTime.getTime()) break shift;
                        shiftSet.add(s);
                }
            }
        }

        workingWeek.setShiftList(shiftSet);

        return workingWeek;
    }
}
