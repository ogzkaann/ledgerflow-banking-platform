import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { mockAuth } from "@/test/auth-mock";
import { AuthGate } from "./AuthGate";

describe("authentication gate", () => {
  beforeEach(() => {
    mockAuth.signinRedirect.mockClear();
    mockAuth.removeUser.mockClear();
  });

  it("replaces protected content with a session-expired recovery state after a 401 event", async () => {
    render(<AuthGate><p>Protected operations</p></AuthGate>);

    expect(screen.getByText("Protected operations")).toBeInTheDocument();
    window.dispatchEvent(new CustomEvent("ledgerflow:session-expired"));

    expect(await screen.findByRole("heading", { name: "Your session has expired" })).toBeVisible();
    expect(screen.queryByText("Protected operations")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Sign in again" }));

    await waitFor(() => {
      expect(mockAuth.removeUser.mock.calls).toHaveLength(1);
      expect(mockAuth.signinRedirect.mock.calls).toHaveLength(1);
    });
  });
});
