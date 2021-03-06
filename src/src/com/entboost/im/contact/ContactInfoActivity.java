package com.entboost.im.contact;

import net.yunim.service.EntboostCache;
import net.yunim.service.EntboostUM;
import net.yunim.service.entity.AppAccountInfo;
import net.yunim.service.entity.ContactInfo;
import net.yunim.service.listener.DelContactListener;
import net.yunim.service.listener.EditContactListener;

import org.apache.commons.lang3.StringUtils;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.entboost.handler.HandlerToolKit;
import com.entboost.im.R;
import com.entboost.im.base.EbActivity;
import com.entboost.im.chat.ChatActivity;
import com.entboost.im.global.UIUtils;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.event.OnClick;

public class ContactInfoActivity extends EbActivity {

	private Long con_id;
	private ContactInfo contactInfo;

	@Override
	protected void onResume() {
		super.onResume();
		init();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setAbContentView(R.layout.activity_contact_info);
		
		ViewUtils.inject(this);
		con_id = getIntent().getLongExtra("con_id", -1);
		
		findViewById(R.id.contact_username).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ContactInfoActivity.this, ContactNameEditActivity.class);
				intent.putExtra("contact", contactInfo.getContact());
				startActivity(intent);
			}
		});
	}

	@OnClick(R.id.contact_proving_btn)
	public void addFriend(View view) {
		if (contactInfo != null) {
			showProgressDialog("正在验证好友");
			EntboostUM.addContact(contactInfo.getContact(), contactInfo.getName(), contactInfo.getDescription(), contactInfo.getUgid(), new EditContactListener() {
				@Override
				public void onFailure(final String errMsg) {
					HandlerToolKit.runOnMainThreadAsync(new Runnable() {
						@Override
						public void run() {
							showToast(errMsg);
							removeProgressDialog();
						}
					});
				}
				
				@Override
				public void onEditContactSuccess() {
					HandlerToolKit.runOnMainThreadAsync(new Runnable() {
						@Override
						public void run() {
							removeProgressDialog();
							finish();
						}
					});
				}
			});
		}
	}
	
	private void init() {
		TextView username = (TextView) findViewById(R.id.contact_username);
		TextView account = (TextView) findViewById(R.id.contact_account);
		TextView conid = (TextView) findViewById(R.id.contact_id);
		TextView conidlab = (TextView) findViewById(R.id.contact_id_lab);
		TextView group = (TextView) findViewById(R.id.contact_group);
		TextView contact_tel = (TextView) findViewById(R.id.contact_tel);
		TextView contact_phone = (TextView) findViewById(R.id.contact_phone);
		TextView contact_email = (TextView) findViewById(R.id.contact_email);
		TextView contact_job_title = (TextView) findViewById(R.id.contact_job_title);
		TextView contact_company = (TextView) findViewById(R.id.contact_company);
		contactInfo = EntboostCache.getContactInfoById(con_id);
		if (contactInfo != null) {
			account.setText(contactInfo.getContact());
			if (contactInfo.getCon_uid() == null) {
				conidlab.setVisibility(View.INVISIBLE);
			} else {
				conid.setText(contactInfo.getCon_uid() + "");
			}
			username.setText(contactInfo.getName());
			group.setText(contactInfo.getGroupName());
			contact_tel.setText(contactInfo.getTel());
			if (contactInfo.getPhone() == null) {
				contact_phone.setText("");
			} else {
				contact_phone.setText(contactInfo.getPhone() + "");
			}
			contact_email.setText(contactInfo.getEmail());
			contact_job_title.setText(contactInfo.getJob_title());
			contact_company.setText(contactInfo.getCompany());
			if (contactInfo.getCon_uid() == null) {
				View send_btn = findViewById(R.id.contact_send_btn);
				send_btn.setVisibility(View.GONE);
			}
			AppAccountInfo appInfo = EntboostCache.getAppInfo();
			// 未验证
			Button del_btn = (Button) findViewById(R.id.contact_del_btn);
			if ((appInfo.getSystem_setting() & AppAccountInfo.SYSTEM_SETTING_VALUE_AUTH_CONTACT) == AppAccountInfo.SYSTEM_SETTING_VALUE_AUTH_CONTACT) {
				del_btn.setText("删除好友");
				if (contactInfo.getType() == 0) {
					Button contact_proving_btn = (Button) findViewById(R.id.contact_proving_btn);
					contact_proving_btn.setVisibility(View.VISIBLE);
				}
			} else {
				del_btn.setText("删除联系人");
			}
		}
	}

	@OnClick(R.id.contact_group_layout)
	public void contact_groupmanager(View view) {
		if (contactInfo != null) {
			Intent intent = new Intent(ContactInfoActivity.this, SelectContactGroupActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra("con_id", contactInfo.getCon_id());
			startActivity(intent);
		}
	}

	@OnClick(R.id.contact_send_btn)
	public void sendMsg(View view) {
		if (contactInfo.getCon_uid() == null) {
			UIUtils.showToast(this, "该联系人不是系统用户，无法进行会话！");
		} else {
			Intent intent = new Intent(ContactInfoActivity.this,
					ChatActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);
			String name = null;
			if (StringUtils.isNotBlank(contactInfo.getName())) {
				name = contactInfo.getName();
			} else {
				name = contactInfo.getContact();
			}
			intent.putExtra(ChatActivity.INTENT_TITLE, name);
			intent.putExtra(ChatActivity.INTENT_UID, contactInfo.getCon_uid());
			startActivity(intent);
		}
	}

	@OnClick(R.id.contact_del_btn)
	public void delContact(View view) {
		showDialog("提示", "确认要删除联系人吗？", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				showProgressDialog("删除联系人");
				EntboostUM.delContact(contactInfo.getCon_id(), new DelContactListener() {
					@Override
					public void onFailure(final String errMsg) {
						HandlerToolKit.runOnMainThreadAsync(new Runnable() {
							@Override
							public void run() {
								pageInfo.showError(errMsg);
								removeProgressDialog();
							}
						});
					}
					@Override
					public void onDelContactSuccess() {
						HandlerToolKit.runOnMainThreadAsync(new Runnable() {
							@Override
							public void run() {
								removeProgressDialog();
								finish();
							}
						});
					}
				});
			}

		});

	}
}
