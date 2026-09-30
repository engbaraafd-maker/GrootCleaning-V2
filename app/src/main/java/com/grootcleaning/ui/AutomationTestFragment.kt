package com.grootcleaning.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.grootcleaning.R
import com.grootcleaning.automation.adapters.SystemUiAdapterRegistry
import com.grootcleaning.automation.detection.AccessibilityBridge
import com.grootcleaning.automation.detection.NodeMatchSpec
import com.grootcleaning.automation.detection.NodeSnapshotMatcher
import com.grootcleaning.automation.detection.TextCatalog
import com.grootcleaning.data.AppCatalog
import com.grootcleaning.service.accessibility.GrootAccessibilityService
import java.util.concurrent.Executors

class AutomationTestFragment: Fragment(){
    private val executor=Executors.newSingleThreadExecutor();private lateinit var log:TextView
    override fun onCreateView(inflater:LayoutInflater,container:ViewGroup?,savedInstanceState:Bundle?):View{
        val c=requireContext();val root=LinearLayout(c).apply{orientation=LinearLayout.VERTICAL;setPadding(16,10,16,20)};val back=Ui.button(c,getString(R.string.back));root.addView(back,LinearLayout.LayoutParams(-1,48));back.setOnClickListener{(activity as?MainActivity)?.show(SettingsFragment())};root.addView(Ui.tv(c,getString(R.string.automation_test),20f,true));root.addView(Ui.tv(c,getString(R.string.test_is_read_only),13f).apply{setTextColor(androidx.core.content.ContextCompat.getColor(c,R.color.gc_warning));setPadding(0,8,0,12)});val run=Ui.button(c,getString(R.string.run_detection_test),true);root.addView(run,LinearLayout.LayoutParams(-1,52));log=Ui.tv(c,"",13f);root.addView(ScrollView(c).apply{addView(log)} ,LinearLayout.LayoutParams(-1,0,1f));run.setOnClickListener{runTest()};return root}
    private fun runTest(){val service=GrootAccessibilityService.INSTANCE;if(service==null){log.text="${getString(R.string.accessibility_status)}: ${getString(R.string.not_detected)}\n${getString(R.string.reason)}: ${getString(R.string.accessibility_disabled)}";return};executor.execute{val c=requireContext();val bridge=AccessibilityBridge(service);val adapter=SystemUiAdapterRegistry.current();val app=AppCatalog(c).load(false).firstOrNull();if(app==null){post("No user app available for detection test.");return@execute};val sb=StringBuilder();sb.append("1. ${getString(R.string.accessibility_status)}: ${getString(R.string.detected)}\n");bridge.open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:${app.packageName}")));val infoOk=bridge.waitFor(12000){nodes->adapter.settingsPackages.any{it==bridge.currentWindowPackage()}&&NodeSnapshotMatcher.anyText(nodes,listOf(app.appName,app.packageName))};sb.append("2. ${getString(R.string.app_info)}: ${if(infoOk)getString(R.string.detected)else getString(R.string.not_detected)}\n");val storageClicked=bridge.click(NodeMatchSpec(texts=adapter.labels.storage,requireEnabled=true));val storageOk=storageClicked&&bridge.waitFor(12000){nodes->NodeSnapshotMatcher.anyText(nodes,adapter.labels.clearCache+adapter.labels.clearData+adapter.labels.manageSpace)};sb.append("3. ${getString(R.string.storage)}: ${if(storageOk)getString(R.string.detected)else getString(R.string.not_detected)}\n");val nodes=bridge.snapshot();sb.append("4. ${getString(R.string.clear_cache_button)}: ${if(NodeSnapshotMatcher.anyText(nodes,TextCatalog.clearCache))getString(R.string.detected)else getString(R.string.not_detected)}\n");sb.append("5. ${getString(R.string.clear_data_button)}: ${if(NodeSnapshotMatcher.anyText(nodes,TextCatalog.clearData))getString(R.string.detected)else getString(R.string.not_detected)}\n");sb.append("6. ${getString(R.string.manage_space_button)}: ${if(NodeSnapshotMatcher.anyText(nodes,TextCatalog.manageSpace))getString(R.string.detected)else getString(R.string.not_detected)}\n");bridge.open(Intent(Settings.ACTION_SYNC_SETTINGS));val accountOk=bridge.waitFor(8000){n->adapter.settingsPackages.any{it==bridge.currentWindowPackage()}&&NodeSnapshotMatcher.anyText(n,listOf("accounts","الحسابات"))};sb.append("7. ${getString(R.string.account_screen)}: ${if(accountOk)getString(R.string.detected)else getString(R.string.not_detected)}\n");post(sb.append("\n${getString(R.string.test_complete)}").toString())}}
    private fun post(s:String){requireActivity().runOnUiThread{log.text=s}}
}
