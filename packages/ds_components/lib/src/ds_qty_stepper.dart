import 'package:flutter/material.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// Bộ tăng/giảm số lượng — dùng trong giỏ hàng (hardcode, không SDUI, nhưng vẫn dùng
/// component chung để đồng bộ theme của shop).
class DsQtyStepper extends StatelessWidget {
  const DsQtyStepper({
    super.key,
    required this.qty,
    required this.onChanged,
    this.min = 1,
    this.max = 99,
  });

  final int qty;
  final ValueChanged<int> onChanged;
  final int min;
  final int max;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _stepButton(t, Icons.remove, qty > min ? () => onChanged(qty - 1) : null),
        SizedBox(
          width: t.space(4),
          child: Text(
            '$qty',
            textAlign: TextAlign.center,
            style: t.textStyle(fontWeight: FontWeight.w600),
          ),
        ),
        _stepButton(t, Icons.add, qty < max ? () => onChanged(qty + 1) : null),
      ],
    );
  }

  Widget _stepButton(DsTokens t, IconData icon, VoidCallback? onTap) {
    return Material(
      color: t.surface,
      shape: RoundedRectangleBorder(
        side: BorderSide(color: t.border),
        borderRadius: BorderRadius.circular(t.radiusSm),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(t.radiusSm),
        child: Padding(
          padding: EdgeInsets.all(t.space(0.75)),
          child: Icon(icon, size: 18, color: onTap == null ? t.muted : t.onSurface),
        ),
      ),
    );
  }
}
