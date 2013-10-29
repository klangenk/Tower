package com.droidplanner.checklist.row;

import com.droidplanner.R;
import com.droidplanner.checklist.CheckListItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class ListRow_Select extends ListRow implements OnItemSelectedListener{
	
	public ListRow_Select( LayoutInflater inflater, CheckListItem checkListItem) {
		super(inflater, checkListItem);
	}

	public View getView(View convertView) {
		View view;
		if (convertView == null) {
			ViewGroup viewGroup = (ViewGroup) inflater.inflate(
					R.layout.list_select_item, null);
			holder = new ViewHolder(viewGroup, checkListItem);
			viewGroup.setTag(holder);
			view = viewGroup;
		} else {
			view = convertView;
			holder = (ViewHolder) convertView.getTag();
		}

		updateDisplay(view, (ViewHolder)holder, checkListItem);
		return view;
	}

	private void updateDisplay(View view, ViewHolder holder,
			CheckListItem mListItem) {
		holder.selectView.setOnItemSelectedListener(this);
		getData(mListItem);

		updateCheckBox(checkListItem.isVerified());
	}

	public int getViewType() {
		return ListRow_Type.SELECT_ROW.ordinal();
	}

	private static class ViewHolder extends BaseViewHolder {
		private Spinner selectView;
		@SuppressWarnings("unused")
		private CheckListItem checkListItem;

		private ArrayAdapter<String> adapter;

		private ViewHolder(ViewGroup viewGroup, CheckListItem checkListItem) {
			super(viewGroup, checkListItem);
			this.checkListItem = checkListItem;			
		}
		
		@Override
		protected void setupViewItems(ViewGroup viewGroup, CheckListItem checkListItem){
			this.selectView = (Spinner) viewGroup.findViewById(R.id.lst_select);

			setupSpinner(viewGroup, checkListItem);
		}

		private void setupSpinner(ViewGroup viewGroup,
				CheckListItem checkListItem) {
			adapter = new ArrayAdapter<String>(viewGroup.getContext(),
					android.R.layout.simple_spinner_item,
					checkListItem.getOptionLists());
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

			selectView.setAdapter(adapter);

		}
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		checkListItem.setSelectedIndex(arg2);
		updateRowChanged(arg1,this.checkListItem);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}

}
