package com.entboost.im.chat;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.yunim.eb.constants.EBConstants;
import net.yunim.service.EntboostCM;
import net.yunim.service.EntboostCache;
import net.yunim.service.api.UserCenter;
import net.yunim.service.entity.AccountInfo;
import net.yunim.service.entity.ChatRoomRichMsg;
import net.yunim.service.entity.FileCache;
import net.yunim.service.entity.MemberInfo;
import net.yunim.utils.ResourceUtils;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.entboost.Log4jLog;
import com.entboost.im.R;
import com.entboost.im.contact.DefaultUserInfoActivity;
import com.entboost.im.global.MyApplication;
import com.entboost.im.global.UIUtils;
import com.entboost.im.group.MemberInfoActivity;
import com.entboost.im.user.UserInfoActivity;
import com.entboost.ui.base.view.popmenu.PopMenu;
import com.entboost.ui.base.view.popmenu.PopMenuItem;
import com.entboost.utils.AbDateUtil;
import com.entboost.voice.ExtAudioRecorder;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ChatMsgViewAdapter extends BaseAdapter {
	private static String LONG_TAG = ChatMsgViewAdapter.class.getName();
	
	private Vector<ChatRoomRichMsg> mChatMsgList = new Vector<ChatRoomRichMsg>();
	private LayoutInflater mInflater;
	private Context context;
	private Map<Long, ProgressBar> progressBarIngs = new ConcurrentHashMap<Long, ProgressBar>();
	private Map<Long, TextView> progressBarIngDescriptions = new ConcurrentHashMap<Long, TextView>();
	private DecimalFormat decimalFormat = new DecimalFormat(".0");// 构造方法的字符格式这里如果小数不足1位,会以0补足.
	private PopMenu popMenu;
	private List<PopMenuItem> popMenuItems;
	
	public void setPopMenu(PopMenu popMenu) {
		this.popMenu = popMenu;
	}

	public void setPopMenuItems(List<PopMenuItem> popMenuItems) {
		this.popMenuItems = popMenuItems;
	}
	
	public void refreshFileProgressBar(FileCache fileCache) {
		ProgressBar progressBar = progressBarIngs.get(fileCache.getMsgId());
		TextView description = progressBarIngDescriptions.get(fileCache.getMsgId());
		if (progressBar != null && description != null) {
			progressBar.setMax((int) fileCache.getTotalSize());
			progressBar.setProgress((int) fileCache.getCurSize());
			description.setVisibility(View.VISIBLE);
			description.setText("下载进度" + fileCache.getPercent() + "  速度：" + fileCache.getCurSpeed());
		}
	}

	public void refreshFileProgressBar(ChatRoomRichMsg msg) {
		ProgressBar progressBar = progressBarIngs.get(msg.getMsgid());
		TextView description = progressBarIngDescriptions.get(msg.getMsgid());
		if (progressBar != null && description != null) {
			progressBar.setMax(100);
			progressBar.setProgress(msg.getPercent().intValue());
			description.setVisibility(View.VISIBLE);
			description.setText("进度：" + decimalFormat.format(msg.getPercent()) + "%");
		}
	}

	public static interface IMsgViewType {
		int IMVT_COM_MSG = 0;	//接收的消息
		int IMVT_TO_MSG = 1;	//发送的消息
	}

	public ChatMsgViewAdapter() {
	}

	public void initChat(Vector<ChatRoomRichMsg> list) {
		this.mChatMsgList.clear();
		this.mChatMsgList.addAll(list);
	}

	public ChatMsgViewAdapter(Context context, Vector<ChatRoomRichMsg> list) {
		this.context = context;
		initChat(list);
		this.mInflater = LayoutInflater.from(context);
	}

	@Override
	public boolean isEnabled(int position) {
		return false;
	}

	@Override
	public int getCount() {
		return mChatMsgList.size();
	}

	@Override
	public Object getItem(int position) {
		return mChatMsgList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		ChatRoomRichMsg mChatMsg = mChatMsgList.get(position);
		if (mChatMsg.getSender() - UserCenter.getInstance().getUserid() == 0) {;
			return IMsgViewType.IMVT_TO_MSG;
		} else {
			return IMsgViewType.IMVT_COM_MSG;
		}
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//是否最后一条记录
		final boolean isLast = (position==mChatMsgList.size()-1);
		//最后一条记录弹出菜单位置偏移量
		final int lastOffset = isLast?-50:0;
		
		final ChatRoomRichMsg mChatMsg = mChatMsgList.get(position);
		final boolean isToMsg = (getItemViewType(position) == IMsgViewType.IMVT_TO_MSG);
		final ViewHolder viewHolder;
		
		if (convertView == null) {
			viewHolder = new ViewHolder();
			
			if (isToMsg) { //
				convertView = mInflater.inflate(R.layout.chatting_item_msg_text_right, null);
				viewHolder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.sendingProgress);
				viewHolder.errorImg = (ImageView) convertView.findViewById(R.id.errorImg);
				viewHolder.errMsg = (TextView) convertView.findViewById(R.id.errMsg);
				viewHolder.file_progressBar = (ProgressBar) convertView.findViewById(R.id.file_progressBar);
				viewHolder.fileBtnLayout = (LinearLayout) convertView.findViewById(R.id.fileBtnLayout);
				viewHolder.file_refuse_btn = (Button) convertView.findViewById(R.id.file_refuse_btn);
				viewHolder.file_offline_btn = (Button) convertView.findViewById(R.id.file_offline_btn);
			} else {
				convertView = mInflater.inflate(R.layout.chatting_item_msg_text_left, null);
				viewHolder.sendName = (TextView) convertView.findViewById(R.id.sendName);
				viewHolder.file_progressBar = (ProgressBar) convertView.findViewById(R.id.file_progressBar);
				viewHolder.fileBtnLayout = (LinearLayout) convertView.findViewById(R.id.fileBtnLayout);
				viewHolder.file_receive_btn = (Button) convertView.findViewById(R.id.file_receive_btn);
				viewHolder.file_refuse_btn = (Button) convertView.findViewById(R.id.file_refuse_btn);
				viewHolder.file_cancel_btn = (Button) convertView.findViewById(R.id.file_cancel_btn);
				viewHolder.file_path_btn = (Button) convertView.findViewById(R.id.file_path_btn);
			}
			
			viewHolder.chatDiscription = (TextView) convertView.findViewById(R.id.chatDiscription);
			viewHolder.sendTime = (TextView) convertView.findViewById(R.id.sendTime);
			viewHolder.chatLayout = (LinearLayout) convertView.findViewById(R.id.chatLayout);
			viewHolder.chatContent = (TextView) convertView.findViewById(R.id.chatContent);
			viewHolder.userHead = (ImageView) convertView.findViewById(R.id.userHead);
			
			if (isToMsg) {
				viewHolder.userHead.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						AccountInfo user = EntboostCache.getUser();
						MemberInfo member = EntboostCache.getMember(
								user.getUid(), mChatMsg.getDepCode());
						if (member != null) {
							Intent intent = new Intent(context, MemberInfoActivity.class);
							intent.putExtra("memberCode", member.getEmp_code());
							intent.putExtra("selfFlag", true);
							context.startActivity(intent);
						} else {
							Intent intent = new Intent(context, UserInfoActivity.class);
							context.startActivity(intent);
						}
					}
				});
			} else {
				viewHolder.userHead.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MemberInfo member = EntboostCache.getMember(mChatMsg.getSender(), mChatMsg.getDepCode());
						if (member != null) {
							Intent intent = new Intent(context, MemberInfoActivity.class);
							intent.putExtra("memberCode", member.getEmp_code());
							context.startActivity(intent);
						} else {
							Intent intent = new Intent(context, DefaultUserInfoActivity.class);
							intent.putExtra("uid", mChatMsg.getSender());
							context.startActivity(intent);
						}
					}
				});
			}
			viewHolder.chatAttach = (ImageView) convertView.findViewById(R.id.chatAttach);
			viewHolder.voicetip = (ImageView) convertView.findViewById(R.id.voicetip);
			viewHolder.isToMsg = isToMsg;
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		viewHolder.file_progressBar.setVisibility(View.GONE);
		viewHolder.file_progressBar.setProgress(0);
		viewHolder.fileBtnLayout.setVisibility(View.GONE);
		viewHolder.chatLayout.setVisibility(View.VISIBLE);
		viewHolder.voicetip.setVisibility(View.GONE);
		viewHolder.chatAttach.setVisibility(View.GONE);
		viewHolder.chatDiscription.setVisibility(View.GONE);
		if (viewHolder.file_path_btn != null) {
			viewHolder.file_path_btn.setVisibility(View.GONE);
		}
		if (viewHolder.file_offline_btn != null) {
			viewHolder.file_offline_btn.setVisibility(View.GONE);
		}
		viewHolder.chatAttach.setOnClickListener(null);
		viewHolder.chatAttach.setFocusable(false);
		
		if (mChatMsgList.size() > 0 && position > 1) {
			ChatRoomRichMsg lastChatMsg = mChatMsgList.get(position - 1);
			if (lastChatMsg == null || AbDateUtil.getOffectMinutes(mChatMsg.getMsgTime(), lastChatMsg.getMsgTime()) >= 1) {
				viewHolder.sendTime.setText(AbDateUtil.formatDateStr2Desc(AbDateUtil.getStringByFormat(mChatMsg.getMsgTime(), AbDateUtil.dateFormatYMDHMS)
						,AbDateUtil.dateFormatYMDHMS));
			} else {
				viewHolder.sendTime.setVisibility(View.GONE);
			}
		} else {
			viewHolder.sendTime.setText(AbDateUtil.formatDateStr2Desc(AbDateUtil.getStringByFormat(mChatMsg.getMsgTime(), AbDateUtil.dateFormatYMDHMS),
					AbDateUtil.dateFormatYMDHMS));
		}
		
		if (isToMsg) {
			if(mChatMsg.getStatus()==EBConstants.SEND_STATUS_ERROR){
				viewHolder.errMsg.setVisibility(View.VISIBLE);
				viewHolder.errMsg.setText(mChatMsg.getErrMsg());
			}else{
				viewHolder.errMsg.setVisibility(View.GONE);
			}
		}
		
		if (mChatMsg.getType() == ChatRoomRichMsg.CHATROOMRICHMSG_TYPE_PIC) { //截图消息
			viewHolder.chatAttach.setVisibility(View.VISIBLE);
			viewHolder.chatLayout.setVisibility(View.GONE);
			viewHolder.chatAttach.setImageBitmap(mChatMsg.getPicBitmap());
			//处理点击事件
			viewHolder.chatAttach.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
//							Intent intent = new Intent(Intent.ACTION_VIEW);
//							intent.setDataAndType(Uri.fromFile(new File(
//									mChatMsg.getFilePath())), "image/*");
//							context.startActivity(intent);
					Intent intent = new Intent(context, FullImageActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra("imgFilePath", mChatMsg.getFilePath());
					context.startActivity(intent);
					((ChatActivity)context).overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out); 
				}
			});
			//处理长按事件
			viewHolder.chatAttach.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					popMenu.removeAllItem();
					popMenu.addItem(popMenuItems.get(1)); //"转发"功能
					popMenu.setObj(mChatMsg);
					popMenu.showAsDropDown(v, lastOffset);
					return false;
				}
			});
		} else if (mChatMsg.getType() == ChatRoomRichMsg.CHATROOMRICHMSG_TYPE_RICH) { //富文本消息
			viewHolder.chatLayout.setVisibility(View.VISIBLE);
			viewHolder.chatAttach.setVisibility(View.GONE);
			viewHolder.chatContent.setText(UIUtils.getTipCharSequence(mChatMsg.getTipHtml()));
			viewHolder.chatContent.setMovementMethod(LinkMovementMethod.getInstance());
			//处理长按事件
			viewHolder.chatContent.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					popMenu.removeAllItem();
					popMenu.addItems(popMenuItems);
					popMenu.setObj(mChatMsg);
					popMenu.showAsDropDown(v, lastOffset);
					return false;
				}
			});
		} else if (mChatMsg.getType() == ChatRoomRichMsg.CHATROOMRICHMSG_TYPE_VOICE) { //语音消息
			viewHolder.voicetip.setVisibility(View.VISIBLE);
			
			//voicetip_gif
			viewHolder.chatLayout.setVisibility(View.VISIBLE);
			viewHolder.chatAttach.setVisibility(View.GONE);
			viewHolder.chatContent.setText(ExtAudioRecorder.getVideoPlayTime(mChatMsg.getFilePath())+"s    ");
			
			//点击播放语音
			OnClickListener listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ExtAudioRecorder.play(mChatMsg.getFilePath());
				}
			};
			viewHolder.chatContent.setOnClickListener(listener);
			viewHolder.chatLayout.setOnClickListener(listener);
			
			//处理长按事件-不支持
//			viewHolder.chatLayout.setOnLongClickListener(new View.OnLongClickListener() {
//				@Override
//				public boolean onLongClick(View v) {
//					popMenu.removeAllItem();
//					popMenu.addItem(popMenuItems.get(1)); //"转发"功能
//					popMenu.setObj(mChatMsg);
//					popMenu.showAsDropDown(v, lastOffset);
//					return false;
//				}
//			});
		} else if (mChatMsg.getType() == ChatRoomRichMsg.CHATROOMRICHMSG_TYPE_FILE) { //文件消息
			if (!isToMsg) {
				if (mChatMsg.getStatus() == EBConstants.RECEIVEFILE_STATUS_DEFAULT) {
					viewHolder.fileBtnLayout.setVisibility(View.VISIBLE);
					viewHolder.file_receive_btn.setVisibility(View.VISIBLE);
					viewHolder.file_refuse_btn.setVisibility(View.VISIBLE);
					viewHolder.file_cancel_btn.setVisibility(View.GONE);
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setVisibility(View.GONE);
					progressBarIngs.remove(mChatMsg.getMsgid());
					progressBarIngDescriptions.remove(mChatMsg.getMsgid());
				} else if (mChatMsg.getStatus() == EBConstants.RECEIVEFILE_STATUS_OFFLINE) {
					viewHolder.fileBtnLayout.setVisibility(View.VISIBLE);
					viewHolder.file_receive_btn.setVisibility(View.VISIBLE);
					viewHolder.file_refuse_btn.setVisibility(View.VISIBLE);
					viewHolder.file_cancel_btn.setVisibility(View.GONE);
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setVisibility(View.GONE);
					progressBarIngs.remove(mChatMsg.getMsgid());
					progressBarIngDescriptions.remove(mChatMsg.getMsgid());
				} else if (mChatMsg.getStatus() == EBConstants.RECEIVEFILE_STATUS_ING) {
					viewHolder.fileBtnLayout.setVisibility(View.VISIBLE);
					viewHolder.file_receive_btn.setVisibility(View.GONE);
					viewHolder.file_refuse_btn.setVisibility(View.GONE);
					viewHolder.file_cancel_btn.setVisibility(View.VISIBLE);
					viewHolder.file_progressBar.setVisibility(View.VISIBLE);
					progressBarIngs.put(mChatMsg.getMsgid(), viewHolder.file_progressBar);
					progressBarIngDescriptions.put(mChatMsg.getMsgid(), viewHolder.chatDiscription);
				} else if (mChatMsg.getStatus() == EBConstants.RECEIVEFILE_STATUS_OK) {
					viewHolder.fileBtnLayout.setVisibility(View.GONE);
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setVisibility(View.VISIBLE);
					viewHolder.chatDiscription.setText("接收完成");
					viewHolder.file_path_btn.setVisibility(View.VISIBLE);
					progressBarIngs.remove(mChatMsg.getMsgid());
					progressBarIngDescriptions.remove(mChatMsg.getMsgid());
					
					viewHolder.file_path_btn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(context, FileListActivity.class);
							context.startActivity(intent);
						}
					});
				} else if (mChatMsg.getStatus() == EBConstants.RECEIVEFILE_STATUS_ERROR) {
					viewHolder.fileBtnLayout.setVisibility(View.GONE);
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setVisibility(View.VISIBLE);
					viewHolder.chatDiscription.setText("接收失败");
					progressBarIngs.remove(mChatMsg.getMsgid());
					progressBarIngDescriptions.remove(mChatMsg.getMsgid());
				} else if (mChatMsg.getStatus() == EBConstants.RECEIVEFILE_STATUS_CANCEL) {
					viewHolder.fileBtnLayout.setVisibility(View.GONE);
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setVisibility(View.VISIBLE);
					viewHolder.chatDiscription.setText("取消接收文件");
					progressBarIngs.remove(mChatMsg.getMsgid());
					progressBarIngDescriptions.remove(mChatMsg.getMsgid());
				}
				
				viewHolder.chatContent.setText(mChatMsg.getText());
				viewHolder.file_cancel_btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						EntboostCM.cancelReceiveFile(mChatMsg);
						viewHolder.fileBtnLayout.setVisibility(View.GONE);
						viewHolder.chatDiscription.setVisibility(View.VISIBLE);
						viewHolder.chatDiscription.setText("取消接收文件");
					}
				});
				
				viewHolder.file_receive_btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mChatMsg.getStatus() == EBConstants.RECEIVEFILE_STATUS_DEFAULT) {
							EntboostCM.acceptFile(mChatMsg);
						} else if (mChatMsg.getStatus() == EBConstants.RECEIVEFILE_STATUS_OFFLINE) {
							EntboostCM.acceptOfflineFile(mChatMsg);
						}
						viewHolder.fileBtnLayout.setVisibility(View.VISIBLE);
						viewHolder.file_receive_btn.setVisibility(View.GONE);
						viewHolder.file_refuse_btn.setVisibility(View.GONE);
						viewHolder.file_cancel_btn.setVisibility(View.VISIBLE);
						viewHolder.file_progressBar.setVisibility(View.VISIBLE);
						progressBarIngs.put(mChatMsg.getMsgid(), viewHolder.file_progressBar);
						progressBarIngDescriptions.put(mChatMsg.getMsgid(), viewHolder.chatDiscription);
					}
				});
				
				viewHolder.file_refuse_btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						EntboostCM.rejectFile(mChatMsg);
						viewHolder.fileBtnLayout.setVisibility(View.GONE);
						viewHolder.chatDiscription.setVisibility(View.VISIBLE);
						viewHolder.chatDiscription.setText("拒绝接收文件");
					}
				});
			} else {
				viewHolder.fileBtnLayout.setVisibility(View.VISIBLE);
				if (mChatMsg.getStatus() == EBConstants.SENDFILE_STATUS_DEFAULT) {
					viewHolder.file_progressBar.setVisibility(View.VISIBLE);
					viewHolder.chatDiscription.setText("等待对方接收文件");
					viewHolder.file_refuse_btn.setVisibility(View.VISIBLE);
					viewHolder.file_offline_btn.setVisibility(View.VISIBLE);
					if (mChatMsg.getMsgid() != null) {
						progressBarIngs.put(mChatMsg.getMsgid(), viewHolder.file_progressBar);
						progressBarIngDescriptions.put(mChatMsg.getMsgid(), viewHolder.chatDiscription);
					}
				} else if (mChatMsg.getStatus() == EBConstants.SENDFILE_STATUS_CANCELTOOFFLIE) {
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setVisibility(View.VISIBLE);
					viewHolder.chatDiscription.setText("取消文件在线发送，已转为离线发送");
					viewHolder.file_refuse_btn.setVisibility(View.GONE);
					viewHolder.file_offline_btn.setVisibility(View.GONE);
					if (mChatMsg.getMsgid() != null) {
						progressBarIngs.remove(mChatMsg.getMsgid());
						progressBarIngDescriptions.remove(mChatMsg.getMsgid());
					}
				} else if (mChatMsg.getStatus() == EBConstants.SENDFILE_STATUS_ING) {
					viewHolder.file_progressBar.setVisibility(View.VISIBLE);
					viewHolder.file_refuse_btn.setVisibility(View.VISIBLE);
					viewHolder.file_offline_btn.setVisibility(View.GONE);
					if (mChatMsg.getMsgid() != null) {
						progressBarIngs.put(mChatMsg.getMsgid(), viewHolder.file_progressBar);
						progressBarIngDescriptions.put(mChatMsg.getMsgid(), viewHolder.chatDiscription);
					}
				} else if (mChatMsg.getStatus() == EBConstants.SENDFILE_STATUS_OK) {
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setVisibility(View.VISIBLE);
					viewHolder.chatDiscription.setText("文件发送完成");
					viewHolder.file_refuse_btn.setVisibility(View.GONE);
					viewHolder.file_offline_btn.setVisibility(View.GONE);
					if (mChatMsg.getMsgid() != null) {
						progressBarIngs.remove(mChatMsg.getMsgid());
						progressBarIngDescriptions.remove(mChatMsg.getMsgid());
					}
				} else if (mChatMsg.getStatus() == EBConstants.SENDFILE_STATUS_ERROR) {
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setText("文件发送失败");
					viewHolder.file_refuse_btn.setVisibility(View.GONE);
					viewHolder.file_offline_btn.setVisibility(View.GONE);
					if (mChatMsg.getMsgid() != null) {
						progressBarIngs.remove(mChatMsg.getMsgid());
						progressBarIngDescriptions.remove(mChatMsg.getMsgid());
					}
				} else if (mChatMsg.getStatus() == EBConstants.SENDFILE_STATUS_CANCEL) {
					viewHolder.file_progressBar.setVisibility(View.GONE);
					viewHolder.chatDiscription.setVisibility(View.VISIBLE);
					viewHolder.chatDiscription.setText("文件发送被取消");
					viewHolder.file_refuse_btn.setVisibility(View.GONE);
					viewHolder.file_offline_btn.setVisibility(View.GONE);
					if (mChatMsg.getMsgid() != null) {
						progressBarIngs.remove(mChatMsg.getMsgid());
						progressBarIngDescriptions.remove(mChatMsg.getMsgid());
					}
				}
				
				viewHolder.chatContent.setText(mChatMsg.getText());
				viewHolder.file_offline_btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						EntboostCM.sendOfflineFile(mChatMsg);
						viewHolder.fileBtnLayout.setVisibility(View.VISIBLE);
						viewHolder.file_progressBar.setVisibility(View.GONE);
						viewHolder.chatDiscription.setVisibility(View.VISIBLE);
						viewHolder.chatDiscription.setText("取消文件在线发送，已转为离线发送");
						viewHolder.file_refuse_btn.setVisibility(View.GONE);
						viewHolder.file_offline_btn.setVisibility(View.GONE);
						if (mChatMsg.getMsgid() != null) {
							progressBarIngs.remove(mChatMsg.getMsgid());
							progressBarIngDescriptions.remove(mChatMsg.getMsgid());
						}
					}
				});
				
				viewHolder.file_refuse_btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						EntboostCM.cancelSendFile(mChatMsg);
						viewHolder.fileBtnLayout.setVisibility(View.VISIBLE);
						viewHolder.file_progressBar.setVisibility(View.GONE);
						viewHolder.chatDiscription.setText("取消文件发送");
						viewHolder.file_refuse_btn.setVisibility(View.GONE);
						viewHolder.file_offline_btn.setVisibility(View.GONE);
						if (mChatMsg.getMsgid() != null) {
							progressBarIngs.remove(mChatMsg.getMsgid());
							progressBarIngDescriptions.remove(mChatMsg.getMsgid());
						}
					}
				});
			}
		}
		
		if (isToMsg) {
			// 显示自己的默认名片头像
			AccountInfo user = EntboostCache.getUser();
			Bitmap img = ResourceUtils.getHeadBitmap(user.getHead_rid());
			if (img != null) {
				viewHolder.userHead.setImageBitmap(img);
			} else {
				ImageLoader.getInstance().displayImage(user.getHeadUrl(), viewHolder.userHead, MyApplication.getInstance().getUserImgOptions());
			}
			if (mChatMsg.getStatus() == EBConstants.SEND_STATUS_ING) {
				viewHolder.mProgressBar.setVisibility(View.VISIBLE);
				viewHolder.errorImg.setVisibility(View.GONE);
			} else if (mChatMsg.getStatus() == EBConstants.SEND_STATUS_OK) {
				viewHolder.mProgressBar.setVisibility(View.GONE);
				viewHolder.errorImg.setVisibility(View.GONE);
			} else if (mChatMsg.getStatus() == EBConstants.SEND_STATUS_ERROR) {
				viewHolder.mProgressBar.setVisibility(View.GONE);
				viewHolder.errorImg.setVisibility(View.VISIBLE);
				viewHolder.errorImg.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((ChatActivity) context).showDialog("提示", "确认要重发消息吗？", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								EntboostCM.reSendMsg(mChatMsg);
							}
						});
					}
				});
			}
		} else {
			// 显示会话对方的默认名片头像
			if (mChatMsg.getChatType() == ChatRoomRichMsg.CHATTYPE_GROUP) {
				viewHolder.sendName.setVisibility(View.VISIBLE);
				viewHolder.sendName.setText(mChatMsg.getSendName());
			}
			Bitmap img = ResourceUtils.getHeadBitmap(mChatMsg.getHid());
			if (img != null) {
				viewHolder.userHead.setImageBitmap(img);
			} else {
				ImageLoader.getInstance().displayImage(mChatMsg.getHeadUrl(), viewHolder.userHead, MyApplication.getInstance().getUserImgOptions());
			}
		}

		return convertView;
	}

	static class ViewHolder {
		public LinearLayout chatLayout;
		public TextView sendTime;
		public TextView errMsg;
		public TextView sendName;
		public TextView chatContent;
		public ImageView userHead;
		public ImageView voicetip;
		public ImageView chatAttach;
		public boolean isToMsg = true;
		public ProgressBar mProgressBar;
		public ImageView errorImg;
		public LinearLayout fileBtnLayout;
		public ProgressBar file_progressBar;
		public Button file_offline_btn;
		public Button file_receive_btn;
		public Button file_refuse_btn;
		public Button file_cancel_btn;
		public TextView chatDiscription;
		public Button file_path_btn;
	}

}
