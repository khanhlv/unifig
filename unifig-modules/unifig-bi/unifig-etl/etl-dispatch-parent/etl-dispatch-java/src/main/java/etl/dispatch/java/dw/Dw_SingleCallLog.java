package etl.dispatch.java.dw;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.tools.plugin.utils.system.IpUtils;

import etl.dispatch.base.holder.SpringContextHolder;
import etl.dispatch.java.datasource.DataSourcePool;
import etl.dispatch.script.AbstractScript;
import etl.dispatch.script.ScriptBean;
import etl.dispatch.script.ScriptCallBack;
import etl.dispatch.script.constant.CommonConstants;
import etl.dispatch.script.util.ScriptTimeUtil;
import etl.dispatch.script.util.SqlUtils;
import etl.dispatch.util.StringUtil;

@Service
public class Dw_SingleCallLog extends AbstractScript {
	private static Logger logger = LoggerFactory.getLogger(Dw_SingleCallLog.class);
	private static final String configJsonPath = "classpath*:conf/json/dw_singleCallLog.json";
	private static final String select_source_storeId = "${store_id}";
	private Optional<Integer> saveDays = Optional.empty();

	private DataSourcePool dataSourcePool;

	public String getName() {
		return "dw.singleCallLog";
	}


	@Override
	protected void start(ScriptBean scriptBean, ScriptCallBack callback) {
		Map<String, Object> paramMap = scriptBean.getParamMap();
		DataSource dataSource = null;
		if (null != paramMap && !paramMap.isEmpty()) {
			Map<String, Object> dataSourceMap = (Map<String, Object>) paramMap.get(CommonConstants.PROP_PARAMS_DATASOURCE);
			if (null != dataSourceMap && !dataSourceMap.isEmpty()) {
				dataSourcePool = SpringContextHolder.getBean("dataSourcePool", DataSourcePool.class);
				dataSource = dataSourcePool.getDataSource(dataSourceMap);
				if (null == dataSource) {
					super.callback(false, "数据源获取失败; dataSource config:" + JSON.toJSONString(dataSourceMap), scriptBean, callback);
				}else{
					logger.info(" Server Ip:"+IpUtils.getIPAddress()+"---> [" + this.getClass().getCanonicalName() + "]; dataSource url:"+ dataSourceMap.get("url"));
				}
			}
			saveDays = Optional.ofNullable((Integer)paramMap.get(CommonConstants.PROP_PARAMS_SAVEDAYS));
		}
		try {
			String optime_yesday = ScriptTimeUtil.optime_yesday();
			String optime_month = optime_yesday.substring(0 , optime_yesday.length() - 2);
			
			// 第一步创建单天(DS)目标表
			String target_ds_sql = super.getJsonConfigValue(configJsonPath, "templateDmTableSql");
			if (!StringUtil.isNullOrEmpty(target_ds_sql)) {
				target_ds_sql = target_ds_sql.replace("dw_engine_singlecall_log_dm_yyyymm", "dw_engine_singlecall_log_dm_" + optime_month);
				SqlUtils.sqlExecute(dataSource, target_ds_sql, this.getName());
			}
			
			// 支持重跑，删除历史数据
			String delete_yes_date = super.getJsonConfigValue(configJsonPath, "delete_yes_date");
			if(!StringUtil.isNullOrEmpty(delete_yes_date)){
				delete_yes_date = delete_yes_date.replace("dw_engine_singlecall_log_dm_yyyymm", "dw_engine_singlecall_log_dm_" + optime_month);
				delete_yes_date = delete_yes_date.replace("${statisDate}", optime_yesday);
				SqlUtils.sqlExecute(dataSource, delete_yes_date, this.getName());
			}
			
			// 第二步插入单天(DS)查询数据
			String inselect_ds_sql = super.getJsonConfigValue(configJsonPath, "insertDmSelectSql");
			if (!StringUtil.isNullOrEmpty(inselect_ds_sql)) {
				inselect_ds_sql = inselect_ds_sql.replace("dw_engine_singlecall_log_dm_yyyymm", "dw_engine_singlecall_log_dm_" + optime_month);
				inselect_ds_sql = inselect_ds_sql.replace("dwd_engine_singlecall_log_dm_yyyymm", "dwd_engine_singlecall_log_dm_" + optime_month);
				inselect_ds_sql = inselect_ds_sql.replace(select_source_storeId, ScriptTimeUtil.optime_yesday());
				SqlUtils.sqlExecute(dataSource, inselect_ds_sql, this.getName());
			}
			
			// 删除历史数据
			String delete_table = super.getJsonConfigValue(configJsonPath, "delete_table");
			String delete_date = super.getJsonConfigValue(configJsonPath, "delete_date");
			if (!StringUtil.isNullOrEmpty(delete_table) && !StringUtil.isNullOrEmpty(delete_date) && saveDays.isPresent()) {
				String delSQl = super.getDelHistorySql(delete_table, delete_date, "dw_engine_singlecall_log_dm_", saveDays.get());
				if(!StringUtil.isNullOrEmpty(delSQl)){
					SqlUtils.sqlExecute(dataSource, delSQl, this.getName());
				}
			}else{
				super.callback(false, "历史数据保留配置异常，saveDays is null;", scriptBean, callback);
			}
			
			// 脚本结束回调状态
			super.callback(true, null, scriptBean, callback);
		} catch (IOException ex) {
			super.callback(false, "config json change JsonParser fail , error:" + ex.getMessage(), scriptBean, callback);
		} catch (SQLException ex) {
			super.callback(false, "fatal error while do java script " + this.getName() + ", DataBase IP :" + super.getUrl(dataSource) + ",message: " + ex.getMessage(), scriptBean, callback);
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}
