import { Check, Copy } from "lucide-react";
import { useState } from "react";

interface CopyButtonProps {
  readonly value: string;
  readonly label?: string;
}

export function CopyButton({ value, label = "Copy" }: CopyButtonProps) {
  const [copied, setCopied] = useState(false);

  return (
    <button
      className="copy-button"
      type="button"
      aria-label={`${label}: ${value}`}
      title={label}
      onClick={() => {
        void navigator.clipboard.writeText(value).then(() => {
          setCopied(true);
          window.setTimeout(() => setCopied(false), 1_500);
        });
      }}
    >
      {copied ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
      <span className="sr-only">{copied ? "Copied" : label}</span>
    </button>
  );
}
