import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import { CheckCircle2, X } from "lucide-react";

interface Feedback {
  readonly id: string;
  readonly message: string;
}

interface FeedbackValue {
  readonly announce: (message: string) => void;
}

const FeedbackContext = createContext<FeedbackValue | null>(null);

export function FeedbackProvider({ children }: PropsWithChildren) {
  const [items, setItems] = useState<Feedback[]>([]);
  const announce = useCallback((message: string) => {
    const item = { id: crypto.randomUUID(), message };
    setItems((current) => [...current, item]);
    window.setTimeout(
      () => setItems((current) => current.filter((candidate) => candidate.id !== item.id)),
      5_000,
    );
  }, []);
  const value = useMemo(() => ({ announce }), [announce]);

  return (
    <FeedbackContext.Provider value={value}>
      {children}
      <div className="toast-region" role="status" aria-live="polite" aria-label="Notifications">
        {items.map((item) => (
          <div className="toast" key={item.id}>
            <CheckCircle2 aria-hidden="true" />
            <span>{item.message}</span>
            <button
              type="button"
              aria-label="Dismiss notification"
              onClick={() =>
                setItems((current) => current.filter((candidate) => candidate.id !== item.id))
              }
            >
              <X aria-hidden="true" />
            </button>
          </div>
        ))}
      </div>
    </FeedbackContext.Provider>
  );
}

export function useFeedback(): FeedbackValue {
  const value = useContext(FeedbackContext);
  if (!value) throw new Error("useFeedback must be used inside FeedbackProvider");
  return value;
}
