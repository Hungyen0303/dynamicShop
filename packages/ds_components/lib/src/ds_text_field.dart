import 'package:flutter/material.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// Ô nhập dùng chung cho checkout (ghi chú, địa chỉ, số điện thoại) — hardcode UI nhưng
/// vẫn ăn theme của shop qua token.
class DsTextField extends StatelessWidget {
  const DsTextField({
    super.key,
    required this.label,
    this.controller,
    this.hint,
    this.maxLines = 1,
    this.keyboardType,
  });

  final String label;
  final TextEditingController? controller;
  final String? hint;
  final int maxLines;
  final TextInputType? keyboardType;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    return TextField(
      controller: controller,
      maxLines: maxLines,
      keyboardType: keyboardType,
      style: t.textStyle(color: t.onSurface),
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        filled: true,
        fillColor: t.surface,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(t.radiusMd),
          borderSide: BorderSide(color: t.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(t.radiusMd),
          borderSide: BorderSide(color: t.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(t.radiusMd),
          borderSide: BorderSide(color: t.primary),
        ),
      ),
    );
  }
}
