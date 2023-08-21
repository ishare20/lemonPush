package net.lemontree.push;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.core.app.DialogCompat;


import java.util.ArrayList;

public class CustomBottomSheetDialog extends Dialog {
    private Context context;
    private ArrayList<String> targetItems;
    private DivideCard.OperationListener operationListener;

    public CustomBottomSheetDialog(Context context, ArrayList<String> targetItems,DivideCard.OperationListener operationListener) {
        super(context);
        this.context = context;
        this.targetItems = targetItems;
        this.operationListener = operationListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.divided_sentence_layout, null);
        setContentView(bottomSheetView);
        ((View) bottomSheetView.getParent()).setBackgroundColor(context.getResources().getColor(R.color.transparent));
        DivideCard divideCard = bottomSheetView.findViewById(R.id.divide_layout);
        divideCard.setOperationListener(operationListener);
        divideCard.setWords(targetItems);
        super.onCreate(savedInstanceState);
    }

}
