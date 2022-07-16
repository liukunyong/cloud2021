package com.atguigu.springcloud.service.impl;

import com.atguigu.springcloud.dao.PaymentDao;
import com.atguigu.springcloud.entities.Payment;
import com.atguigu.springcloud.service.PaymentService;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.apache.skywalking.apm.toolkit.trace.Tag;
import org.apache.skywalking.apm.toolkit.trace.Tags;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 */
@Service
public class PaymentServiceImpl implements PaymentService
{
    @Resource
    private PaymentDao paymentDao;

    public int create(Payment payment)
    {
        return paymentDao.create(payment);
    }

    @Trace
    @Tag(key="getPaymentById",value = "returnedObj")
    @Tags(@Tag(key="getPaymentById",value = "arg[0]"))
    public Payment getPaymentById(Long id)
    {
        return paymentDao.getPaymentById(id);
    }

    @Override
    public List<Payment> getAll() {
        return paymentDao.getAll();
    }

    @Override
    public void download(String report, HttpServletResponse response) {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        InputStream inp = null;
        String fileName = report.replace("-","")+"dddd";

        try {
            inp = this.getClass().getClassLoader().getResourceAsStream("download/nationalDebtReportTemp.xlsx");
            XSSFWorkbook wb = XSSFWorkbookFactory.createWorkbook(inp);
            XSSFSheet sheet = wb.getSheet("");
            XSSFRow row = sheet.getRow(0);
            String fileNameRes = URLEncoder.encode(fileName,"utf-8");
            response.setHeader("Content-dispostion","attachment;filename="+fileNameRes+".xlsx");
            String osNmae = System.getProperties().getProperty("os.name");
            OutputStream out ;
            if("Linux".equals(osNmae)){
                out = response.getOutputStream();
            }else {
                out = new FileOutputStream("D:\\2345Downloads\\"+fileName+".xlsx");
            }
            wb.write(out);
            wb.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

