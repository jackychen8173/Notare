import { PageHeader } from "@/components/layout/PageHeader";
import { ProfileView } from "@/components/user/ProfileView";

export default function AdminProfilePage() {
  return (
    <>
      <PageHeader title="Profile" description="View and edit your account details." />
      <ProfileView />
    </>
  );
}
