package com.company.flow.sys.flowtask.service;

import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.mapper.UserMapper;
import com.company.flow.sys.flowtask.vo.ImportMemberVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 批量导入人员（Excel）：生成导入模板 + 解析上传文件并按用户号校验。
 *
 * 校验规则（任一不通过则整批拒绝，一次性返回全部问题行）：
 * 1) 用户号非空、且在 aut_user 中存在（含停用账号视为不存在）；
 * 2) 文件内用户号不重复；
 * 3) 填了姓名的，必须与该用户号在库中的姓名一致（留空则不校验姓名）。
 *
 * 注意：姓名与部门一律以库中数据为准返回，不采信 Excel 里写的姓名（避免导入脏数据）。
 */
@Service
public class MemberImportService {

    /** Excel 表头文案 */
    private static final String[] HEADERS = {"用户号", "用户姓名", "任务名（选填）"};
    /** 说明行前缀（解析时按前缀跳过，用户删掉这一行也不影响） */
    private static final String TIP_PREFIX = "说明：";
    private static final String TIP_TEXT = "说明：用户号必填，且必须是系统内的真实账号；用户姓名需与用户号一致（填了就会校验，留空则不校验）；"
            + "任务名留空时自动使用「下发给{姓名}的任务」。请从第 3 行开始填写，本行可以删除。示例：emp0005 / 钱七 / 留空";

    @Autowired
    private UserMapper userMapper;

    /** 生成导入模板（xlsx），直接写到响应流 */
    public void writeTemplate(OutputStream out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("人员导入");

            CellStyle headStyle = wb.createCellStyle();
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row head = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = head.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headStyle);
            }

            CellStyle tipStyle = wb.createCellStyle();
            Font tipFont = wb.createFont();
            tipFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            tipFont.setFontHeightInPoints((short) 10);
            tipStyle.setFont(tipFont);
            Cell tip = sheet.createRow(1).createCell(0);
            tip.setCellValue(TIP_TEXT);
            tip.setCellStyle(tipStyle);

            sheet.setColumnWidth(0, 20 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 34 * 256);
            wb.write(out);
        }
    }

    /** 解析并校验导入文件；通过则返回人员列表（姓名/部门以库中为准），否则抛异常并携带全部问题行 */
    public List<ImportMemberVO> parseAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new RuntimeException("请选择要导入的 Excel 文件");
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new RuntimeException("仅支持 .xlsx / .xls 格式的 Excel 文件");
        }

        List<ImportMemberVO> result = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Map<String, User> userCache = new HashMap<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new RuntimeException("Excel 内容为空");
            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {   // 第 1 行是表头，跳过
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String yyytId = cellText(row.getCell(0)).trim();
                String declaredName = cellText(row.getCell(1)).trim();
                String taskName = cellText(row.getCell(2)).trim();
                // 跳过说明行与空行
                if (yyytId.startsWith(TIP_PREFIX) || yyytId.startsWith("说明")) continue;
                if (yyytId.isEmpty() && declaredName.isEmpty() && taskName.isEmpty()) continue;

                int rowNo = r + 1;
                if (yyytId.isEmpty()) {
                    errors.add("第 " + rowNo + " 行：用户号不能为空");
                    continue;
                }
                if (!seenIds.add(yyytId)) {
                    errors.add("第 " + rowNo + " 行：用户号「" + yyytId + "」在文件内重复");
                    continue;
                }
                User u = userCache.computeIfAbsent(yyytId, k -> userMapper.selectById(k));
                if (u == null || (u.getStatus() != null && u.getStatus() != 1)) {
                    errors.add("第 " + rowNo + " 行：用户号「" + yyytId + "」在系统中不存在或已停用");
                    continue;
                }
                if (!declaredName.isEmpty() && !declaredName.equals(u.getUserName())) {
                    errors.add("第 " + rowNo + " 行：姓名「" + declaredName + "」与用户号「" + yyytId + "」不匹配"
                            + "（该账号在系统中的姓名是「" + u.getUserName() + "」）");
                    continue;
                }
                ImportMemberVO vo = new ImportMemberVO();
                vo.setYyytId(u.getYyytId());
                vo.setUserName(u.getUserName());
                vo.setDeptName(u.getDeptName());
                vo.setTaskName(taskName.isEmpty() ? "下发给" + u.getUserName() + "的任务" : taskName);
                result.add(vo);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Excel 解析失败：请确认文件未损坏且为标准的模板格式");
        }

        if (errors.isEmpty() && result.isEmpty()) {
            throw new RuntimeException("Excel 中没有可导入的人员，请按模板从第 3 行开始填写");
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException("导入校验未通过，共 " + errors.size() + " 处问题，本次未导入任何人员：\n" + String.join("\n", errors));
        }
        return result;
    }

    /** 读取单元格文本（数字型用户号按整数输出，避免出现科学计数法） */
    private String cellText(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                return d == Math.floor(d) && !Double.isInfinite(d) ? String.valueOf((long) d) : String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
