import { screen } from "@testing-library/react";
import { render } from "@testing-library/react";
import { historyFixture } from "@/test/fixtures";
import { TransferTimeline } from "./TransferTimeline";

describe("TransferTimeline", () => {
  it("renders only persisted completed-path transitions", () => {
    render(<TransferTimeline history={historyFixture} currentStatus="COMPLETED" />);
    expect(screen.getAllByRole("listitem")).toHaveLength(5);
    expect(screen.getByText("SETTLING")).toBeInTheDocument();
    expect(screen.queryByText("COMPENSATING")).not.toBeInTheDocument();
    expect(screen.queryByText("Workflow processing")).not.toBeInTheDocument();
  });

  it("announces an active workflow without inventing a transition", () => {
    render(<TransferTimeline history={historyFixture.slice(0, 2)} currentStatus="FUNDS_RESERVED" />);
    expect(screen.getByText("Workflow processing")).toBeInTheDocument();
  });
});
