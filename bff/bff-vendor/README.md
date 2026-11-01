# Vendor portal BFF (`bff-vendor`)

| | |
|---|---|
| Owning PRDs | [V01](../../spec/prd/05-vendor/PRD-V01-vendor-management.md) |
| HTTP port (local) | `8203` |
| Deployable | `:bff:bff-vendor` |

Backend for the vendor portal: assignments, uploads, milestones, invoices.

A BFF composes bounded-context APIs for one client. It holds **no domain logic**: it never enforces business
rules, computes money, or decides state (CRC-ARCH-011). It may depend only on context `-api` modules and
platform libraries; the build and `VendorBffContractTest` enforce this.
