"use client";

import { useState } from "react";
import Link from "next/link";
import { IconLogout, IconMessageReport, IconUser, IconUserCircle } from "@tabler/icons-react";

import { ReportProblemDialog } from "@/components/layout/ReportProblemDialog";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useSignOut } from "@/hooks/useSignOut";
import { getSession } from "@/lib/auth";

export function AccountMenu({ profileHref }: { profileHref: string }) {
  const signOut = useSignOut();
  const [reportOpen, setReportOpen] = useState(false);
  // AppShell only renders after useAuthGuard has confirmed a session on the client, so reading
  // localStorage here never runs during SSR.
  const [session] = useState(getSession);

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger
          render={
            <Button variant="ghost" size="icon" aria-label="Account menu">
              <IconUserCircle className="size-5" stroke={1.75} />
            </Button>
          }
        />
        <DropdownMenuContent align="end" className="w-60">
          {session ? (
            <DropdownMenuGroup>
              <DropdownMenuLabel className="flex flex-col gap-0.5 px-2 py-1.5">
                <span className="truncate text-sm font-medium text-foreground">{session.name}</span>
                <span className="truncate text-xs font-normal">
                  {session.demo ? "Demo account" : session.email}
                </span>
              </DropdownMenuLabel>
            </DropdownMenuGroup>
          ) : null}
          <DropdownMenuSeparator />
          <DropdownMenuItem render={<Link href={profileHref} />}>
            <IconUser stroke={1.75} />
            Profile
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setReportOpen(true)}>
            <IconMessageReport stroke={1.75} />
            Report a problem
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onClick={() => signOut()}>
            <IconLogout stroke={1.75} />
            Sign out
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <ReportProblemDialog open={reportOpen} onOpenChange={setReportOpen} />
    </>
  );
}
