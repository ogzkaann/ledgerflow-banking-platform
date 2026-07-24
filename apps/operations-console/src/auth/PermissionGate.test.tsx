import { screen } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { renderConsole } from "@/test/render";
import { setMockRole } from "@/test/auth-mock";
import { WritePermissionGate } from "./PermissionGate";

describe("write route guard", () => {
  it("redirects an auditor away from a write route", async () => {
    setMockRole("ledgerflow-auditor");
    renderConsole(<Routes><Route path="/" element={<p>Overview</p>} /><Route path="/write" element={<WritePermissionGate><p>Write screen</p></WritePermissionGate>} /></Routes>, "/write");
    expect(await screen.findByText("Overview")).toBeInTheDocument();
    expect(screen.queryByText("Write screen")).not.toBeInTheDocument();
  });

  it("allows an operator while backend authorization remains authoritative", () => {
    setMockRole("ledgerflow-operator");
    renderConsole(<Routes><Route path="/write" element={<WritePermissionGate><p>Write screen</p></WritePermissionGate>} /></Routes>, "/write");
    expect(screen.getByText("Write screen")).toBeInTheDocument();
  });
});
