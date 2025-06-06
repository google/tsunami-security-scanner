//
// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Representation of a tsunami plugin definition passed between language
// servers.

// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.36.5
// 	protoc        v3.21.12
// source: plugin_representation.proto

package tsunami_go_proto

import (
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
	sync "sync"
	unsafe "unsafe"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

type PluginInfo_PluginType int32

const (
	// Plugin is an unspecified type.
	PluginInfo_PLUGIN_TYPE_UNSPECIFIED PluginInfo_PluginType = 0
	// Plugin is a port scanner.
	PluginInfo_PORT_SCAN PluginInfo_PluginType = 1
	// Plugin is a service fingerprinter.
	PluginInfo_SERVICE_FINGERPRINT PluginInfo_PluginType = 2
	// Plugin is a vulnerability detector.
	PluginInfo_VULN_DETECTION PluginInfo_PluginType = 3
)

// Enum value maps for PluginInfo_PluginType.
var (
	PluginInfo_PluginType_name = map[int32]string{
		0: "PLUGIN_TYPE_UNSPECIFIED",
		1: "PORT_SCAN",
		2: "SERVICE_FINGERPRINT",
		3: "VULN_DETECTION",
	}
	PluginInfo_PluginType_value = map[string]int32{
		"PLUGIN_TYPE_UNSPECIFIED": 0,
		"PORT_SCAN":               1,
		"SERVICE_FINGERPRINT":     2,
		"VULN_DETECTION":          3,
	}
)

func (x PluginInfo_PluginType) Enum() *PluginInfo_PluginType {
	p := new(PluginInfo_PluginType)
	*p = x
	return p
}

func (x PluginInfo_PluginType) String() string {
	return protoimpl.X.EnumStringOf(x.Descriptor(), protoreflect.EnumNumber(x))
}

func (PluginInfo_PluginType) Descriptor() protoreflect.EnumDescriptor {
	return file_plugin_representation_proto_enumTypes[0].Descriptor()
}

func (PluginInfo_PluginType) Type() protoreflect.EnumType {
	return &file_plugin_representation_proto_enumTypes[0]
}

func (x PluginInfo_PluginType) Number() protoreflect.EnumNumber {
	return protoreflect.EnumNumber(x)
}

// Deprecated: Use PluginInfo_PluginType.Descriptor instead.
func (PluginInfo_PluginType) EnumDescriptor() ([]byte, []int) {
	return file_plugin_representation_proto_rawDescGZIP(), []int{1, 0}
}

// Represents a PluginDefinition placeholder.
type PluginDefinition struct {
	state protoimpl.MessageState `protogen:"open.v1"`
	// PluginInfo of this definition.
	Info *PluginInfo `protobuf:"bytes,1,opt,name=info,proto3" json:"info,omitempty"`
	// The name of the target service.
	TargetServiceName *TargetServiceName `protobuf:"bytes,2,opt,name=target_service_name,json=targetServiceName,proto3" json:"target_service_name,omitempty"`
	// The name of the target software.
	TargetSoftware *TargetSoftware `protobuf:"bytes,3,opt,name=target_software,json=targetSoftware,proto3" json:"target_software,omitempty"`
	// If the definition is for a web service or not.
	ForWebService bool `protobuf:"varint,4,opt,name=for_web_service,json=forWebService,proto3" json:"for_web_service,omitempty"`
	// If the definition is for a specific operating system or not.
	// Note: this filter is executed within an AND condition with the other
	// filters. E.g. if target_service_name.value is "http" and
	// target_operating_system.osclass.family is "Linux" then the plugin will only
	// match if the service is http and the operating system is Linux.
	TargetOperatingSystemClass *TargetOperatingSystemClass `protobuf:"bytes,5,opt,name=target_operating_system_class,json=targetOperatingSystemClass,proto3" json:"target_operating_system_class,omitempty"`
	unknownFields              protoimpl.UnknownFields
	sizeCache                  protoimpl.SizeCache
}

func (x *PluginDefinition) Reset() {
	*x = PluginDefinition{}
	mi := &file_plugin_representation_proto_msgTypes[0]
	ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
	ms.StoreMessageInfo(mi)
}

func (x *PluginDefinition) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*PluginDefinition) ProtoMessage() {}

func (x *PluginDefinition) ProtoReflect() protoreflect.Message {
	mi := &file_plugin_representation_proto_msgTypes[0]
	if x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use PluginDefinition.ProtoReflect.Descriptor instead.
func (*PluginDefinition) Descriptor() ([]byte, []int) {
	return file_plugin_representation_proto_rawDescGZIP(), []int{0}
}

func (x *PluginDefinition) GetInfo() *PluginInfo {
	if x != nil {
		return x.Info
	}
	return nil
}

func (x *PluginDefinition) GetTargetServiceName() *TargetServiceName {
	if x != nil {
		return x.TargetServiceName
	}
	return nil
}

func (x *PluginDefinition) GetTargetSoftware() *TargetSoftware {
	if x != nil {
		return x.TargetSoftware
	}
	return nil
}

func (x *PluginDefinition) GetForWebService() bool {
	if x != nil {
		return x.ForWebService
	}
	return false
}

func (x *PluginDefinition) GetTargetOperatingSystemClass() *TargetOperatingSystemClass {
	if x != nil {
		return x.TargetOperatingSystemClass
	}
	return nil
}

// Represents a PluginInfo annotation placeholder used by the
// PluginDefinition proto above.
type PluginInfo struct {
	state protoimpl.MessageState `protogen:"open.v1"`
	// Type of plugin.
	Type PluginInfo_PluginType `protobuf:"varint,1,opt,name=type,proto3,enum=tsunami.proto.PluginInfo_PluginType" json:"type,omitempty"`
	// Name of the plugin.
	Name string `protobuf:"bytes,2,opt,name=name,proto3" json:"name,omitempty"`
	// Version of the plugin
	Version string `protobuf:"bytes,3,opt,name=version,proto3" json:"version,omitempty"`
	// Description of the plugin.
	Description string `protobuf:"bytes,4,opt,name=description,proto3" json:"description,omitempty"`
	// Author of the plugin.
	Author        string `protobuf:"bytes,5,opt,name=author,proto3" json:"author,omitempty"`
	unknownFields protoimpl.UnknownFields
	sizeCache     protoimpl.SizeCache
}

func (x *PluginInfo) Reset() {
	*x = PluginInfo{}
	mi := &file_plugin_representation_proto_msgTypes[1]
	ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
	ms.StoreMessageInfo(mi)
}

func (x *PluginInfo) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*PluginInfo) ProtoMessage() {}

func (x *PluginInfo) ProtoReflect() protoreflect.Message {
	mi := &file_plugin_representation_proto_msgTypes[1]
	if x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use PluginInfo.ProtoReflect.Descriptor instead.
func (*PluginInfo) Descriptor() ([]byte, []int) {
	return file_plugin_representation_proto_rawDescGZIP(), []int{1}
}

func (x *PluginInfo) GetType() PluginInfo_PluginType {
	if x != nil {
		return x.Type
	}
	return PluginInfo_PLUGIN_TYPE_UNSPECIFIED
}

func (x *PluginInfo) GetName() string {
	if x != nil {
		return x.Name
	}
	return ""
}

func (x *PluginInfo) GetVersion() string {
	if x != nil {
		return x.Version
	}
	return ""
}

func (x *PluginInfo) GetDescription() string {
	if x != nil {
		return x.Description
	}
	return ""
}

func (x *PluginInfo) GetAuthor() string {
	if x != nil {
		return x.Author
	}
	return ""
}

// Represents a ForServiceName annotation placeholder used by the
// PluginDefinition proto above.
type TargetServiceName struct {
	state protoimpl.MessageState `protogen:"open.v1"`
	// The value of the name of the target.
	Value         []string `protobuf:"bytes,1,rep,name=value,proto3" json:"value,omitempty"`
	unknownFields protoimpl.UnknownFields
	sizeCache     protoimpl.SizeCache
}

func (x *TargetServiceName) Reset() {
	*x = TargetServiceName{}
	mi := &file_plugin_representation_proto_msgTypes[2]
	ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
	ms.StoreMessageInfo(mi)
}

func (x *TargetServiceName) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*TargetServiceName) ProtoMessage() {}

func (x *TargetServiceName) ProtoReflect() protoreflect.Message {
	mi := &file_plugin_representation_proto_msgTypes[2]
	if x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use TargetServiceName.ProtoReflect.Descriptor instead.
func (*TargetServiceName) Descriptor() ([]byte, []int) {
	return file_plugin_representation_proto_rawDescGZIP(), []int{2}
}

func (x *TargetServiceName) GetValue() []string {
	if x != nil {
		return x.Value
	}
	return nil
}

// Represents a ForSoftware annotation placeholder used by the
// PluginDefinition proto above.
type TargetSoftware struct {
	state protoimpl.MessageState `protogen:"open.v1"`
	// The name of the target software, case insensitive.
	Name string `protobuf:"bytes,1,opt,name=name,proto3" json:"name,omitempty"`
	// Array of versions and version ranges of the target software.
	Value         []string `protobuf:"bytes,2,rep,name=value,proto3" json:"value,omitempty"`
	unknownFields protoimpl.UnknownFields
	sizeCache     protoimpl.SizeCache
}

func (x *TargetSoftware) Reset() {
	*x = TargetSoftware{}
	mi := &file_plugin_representation_proto_msgTypes[3]
	ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
	ms.StoreMessageInfo(mi)
}

func (x *TargetSoftware) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*TargetSoftware) ProtoMessage() {}

func (x *TargetSoftware) ProtoReflect() protoreflect.Message {
	mi := &file_plugin_representation_proto_msgTypes[3]
	if x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use TargetSoftware.ProtoReflect.Descriptor instead.
func (*TargetSoftware) Descriptor() ([]byte, []int) {
	return file_plugin_representation_proto_rawDescGZIP(), []int{3}
}

func (x *TargetSoftware) GetName() string {
	if x != nil {
		return x.Name
	}
	return ""
}

func (x *TargetSoftware) GetValue() []string {
	if x != nil {
		return x.Value
	}
	return nil
}

// Represents a ForOperatingSystem annotation placeholder used by the
// PluginDefinition proto above. These values are coming directly from the
// port scanner's output (e.g. nmap).
type TargetOperatingSystemClass struct {
	state protoimpl.MessageState `protogen:"open.v1"`
	// The vendor of the target operating system, e.g. "Microsoft"
	Vendor []string `protobuf:"bytes,1,rep,name=vendor,proto3" json:"vendor,omitempty"`
	// The family of the target operating system, e.g. "Windows"
	OsFamily []string `protobuf:"bytes,2,rep,name=os_family,json=osFamily,proto3" json:"os_family,omitempty"`
	// The minimum accuracy of the target operating system, e.g. 90
	MinAccuracy   uint32 `protobuf:"varint,3,opt,name=min_accuracy,json=minAccuracy,proto3" json:"min_accuracy,omitempty"`
	unknownFields protoimpl.UnknownFields
	sizeCache     protoimpl.SizeCache
}

func (x *TargetOperatingSystemClass) Reset() {
	*x = TargetOperatingSystemClass{}
	mi := &file_plugin_representation_proto_msgTypes[4]
	ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
	ms.StoreMessageInfo(mi)
}

func (x *TargetOperatingSystemClass) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*TargetOperatingSystemClass) ProtoMessage() {}

func (x *TargetOperatingSystemClass) ProtoReflect() protoreflect.Message {
	mi := &file_plugin_representation_proto_msgTypes[4]
	if x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use TargetOperatingSystemClass.ProtoReflect.Descriptor instead.
func (*TargetOperatingSystemClass) Descriptor() ([]byte, []int) {
	return file_plugin_representation_proto_rawDescGZIP(), []int{4}
}

func (x *TargetOperatingSystemClass) GetVendor() []string {
	if x != nil {
		return x.Vendor
	}
	return nil
}

func (x *TargetOperatingSystemClass) GetOsFamily() []string {
	if x != nil {
		return x.OsFamily
	}
	return nil
}

func (x *TargetOperatingSystemClass) GetMinAccuracy() uint32 {
	if x != nil {
		return x.MinAccuracy
	}
	return 0
}

var File_plugin_representation_proto protoreflect.FileDescriptor

var file_plugin_representation_proto_rawDesc = string([]byte{
	0x0a, 0x1b, 0x70, 0x6c, 0x75, 0x67, 0x69, 0x6e, 0x5f, 0x72, 0x65, 0x70, 0x72, 0x65, 0x73, 0x65,
	0x6e, 0x74, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x12, 0x0d, 0x74,
	0x73, 0x75, 0x6e, 0x61, 0x6d, 0x69, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x22, 0xf1, 0x02, 0x0a,
	0x10, 0x50, 0x6c, 0x75, 0x67, 0x69, 0x6e, 0x44, 0x65, 0x66, 0x69, 0x6e, 0x69, 0x74, 0x69, 0x6f,
	0x6e, 0x12, 0x2d, 0x0a, 0x04, 0x69, 0x6e, 0x66, 0x6f, 0x18, 0x01, 0x20, 0x01, 0x28, 0x0b, 0x32,
	0x19, 0x2e, 0x74, 0x73, 0x75, 0x6e, 0x61, 0x6d, 0x69, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x2e,
	0x50, 0x6c, 0x75, 0x67, 0x69, 0x6e, 0x49, 0x6e, 0x66, 0x6f, 0x52, 0x04, 0x69, 0x6e, 0x66, 0x6f,
	0x12, 0x50, 0x0a, 0x13, 0x74, 0x61, 0x72, 0x67, 0x65, 0x74, 0x5f, 0x73, 0x65, 0x72, 0x76, 0x69,
	0x63, 0x65, 0x5f, 0x6e, 0x61, 0x6d, 0x65, 0x18, 0x02, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x20, 0x2e,
	0x74, 0x73, 0x75, 0x6e, 0x61, 0x6d, 0x69, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x2e, 0x54, 0x61,
	0x72, 0x67, 0x65, 0x74, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x4e, 0x61, 0x6d, 0x65, 0x52,
	0x11, 0x74, 0x61, 0x72, 0x67, 0x65, 0x74, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x4e, 0x61,
	0x6d, 0x65, 0x12, 0x46, 0x0a, 0x0f, 0x74, 0x61, 0x72, 0x67, 0x65, 0x74, 0x5f, 0x73, 0x6f, 0x66,
	0x74, 0x77, 0x61, 0x72, 0x65, 0x18, 0x03, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x1d, 0x2e, 0x74, 0x73,
	0x75, 0x6e, 0x61, 0x6d, 0x69, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x2e, 0x54, 0x61, 0x72, 0x67,
	0x65, 0x74, 0x53, 0x6f, 0x66, 0x74, 0x77, 0x61, 0x72, 0x65, 0x52, 0x0e, 0x74, 0x61, 0x72, 0x67,
	0x65, 0x74, 0x53, 0x6f, 0x66, 0x74, 0x77, 0x61, 0x72, 0x65, 0x12, 0x26, 0x0a, 0x0f, 0x66, 0x6f,
	0x72, 0x5f, 0x77, 0x65, 0x62, 0x5f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x18, 0x04, 0x20,
	0x01, 0x28, 0x08, 0x52, 0x0d, 0x66, 0x6f, 0x72, 0x57, 0x65, 0x62, 0x53, 0x65, 0x72, 0x76, 0x69,
	0x63, 0x65, 0x12, 0x6c, 0x0a, 0x1d, 0x74, 0x61, 0x72, 0x67, 0x65, 0x74, 0x5f, 0x6f, 0x70, 0x65,
	0x72, 0x61, 0x74, 0x69, 0x6e, 0x67, 0x5f, 0x73, 0x79, 0x73, 0x74, 0x65, 0x6d, 0x5f, 0x63, 0x6c,
	0x61, 0x73, 0x73, 0x18, 0x05, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x29, 0x2e, 0x74, 0x73, 0x75, 0x6e,
	0x61, 0x6d, 0x69, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x2e, 0x54, 0x61, 0x72, 0x67, 0x65, 0x74,
	0x4f, 0x70, 0x65, 0x72, 0x61, 0x74, 0x69, 0x6e, 0x67, 0x53, 0x79, 0x73, 0x74, 0x65, 0x6d, 0x43,
	0x6c, 0x61, 0x73, 0x73, 0x52, 0x1a, 0x74, 0x61, 0x72, 0x67, 0x65, 0x74, 0x4f, 0x70, 0x65, 0x72,
	0x61, 0x74, 0x69, 0x6e, 0x67, 0x53, 0x79, 0x73, 0x74, 0x65, 0x6d, 0x43, 0x6c, 0x61, 0x73, 0x73,
	0x22, 0x95, 0x02, 0x0a, 0x0a, 0x50, 0x6c, 0x75, 0x67, 0x69, 0x6e, 0x49, 0x6e, 0x66, 0x6f, 0x12,
	0x38, 0x0a, 0x04, 0x74, 0x79, 0x70, 0x65, 0x18, 0x01, 0x20, 0x01, 0x28, 0x0e, 0x32, 0x24, 0x2e,
	0x74, 0x73, 0x75, 0x6e, 0x61, 0x6d, 0x69, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x2e, 0x50, 0x6c,
	0x75, 0x67, 0x69, 0x6e, 0x49, 0x6e, 0x66, 0x6f, 0x2e, 0x50, 0x6c, 0x75, 0x67, 0x69, 0x6e, 0x54,
	0x79, 0x70, 0x65, 0x52, 0x04, 0x74, 0x79, 0x70, 0x65, 0x12, 0x12, 0x0a, 0x04, 0x6e, 0x61, 0x6d,
	0x65, 0x18, 0x02, 0x20, 0x01, 0x28, 0x09, 0x52, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x12, 0x18, 0x0a,
	0x07, 0x76, 0x65, 0x72, 0x73, 0x69, 0x6f, 0x6e, 0x18, 0x03, 0x20, 0x01, 0x28, 0x09, 0x52, 0x07,
	0x76, 0x65, 0x72, 0x73, 0x69, 0x6f, 0x6e, 0x12, 0x20, 0x0a, 0x0b, 0x64, 0x65, 0x73, 0x63, 0x72,
	0x69, 0x70, 0x74, 0x69, 0x6f, 0x6e, 0x18, 0x04, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0b, 0x64, 0x65,
	0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f, 0x6e, 0x12, 0x16, 0x0a, 0x06, 0x61, 0x75, 0x74,
	0x68, 0x6f, 0x72, 0x18, 0x05, 0x20, 0x01, 0x28, 0x09, 0x52, 0x06, 0x61, 0x75, 0x74, 0x68, 0x6f,
	0x72, 0x22, 0x65, 0x0a, 0x0a, 0x50, 0x6c, 0x75, 0x67, 0x69, 0x6e, 0x54, 0x79, 0x70, 0x65, 0x12,
	0x1b, 0x0a, 0x17, 0x50, 0x4c, 0x55, 0x47, 0x49, 0x4e, 0x5f, 0x54, 0x59, 0x50, 0x45, 0x5f, 0x55,
	0x4e, 0x53, 0x50, 0x45, 0x43, 0x49, 0x46, 0x49, 0x45, 0x44, 0x10, 0x00, 0x12, 0x0d, 0x0a, 0x09,
	0x50, 0x4f, 0x52, 0x54, 0x5f, 0x53, 0x43, 0x41, 0x4e, 0x10, 0x01, 0x12, 0x17, 0x0a, 0x13, 0x53,
	0x45, 0x52, 0x56, 0x49, 0x43, 0x45, 0x5f, 0x46, 0x49, 0x4e, 0x47, 0x45, 0x52, 0x50, 0x52, 0x49,
	0x4e, 0x54, 0x10, 0x02, 0x12, 0x12, 0x0a, 0x0e, 0x56, 0x55, 0x4c, 0x4e, 0x5f, 0x44, 0x45, 0x54,
	0x45, 0x43, 0x54, 0x49, 0x4f, 0x4e, 0x10, 0x03, 0x22, 0x29, 0x0a, 0x11, 0x54, 0x61, 0x72, 0x67,
	0x65, 0x74, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x4e, 0x61, 0x6d, 0x65, 0x12, 0x14, 0x0a,
	0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x18, 0x01, 0x20, 0x03, 0x28, 0x09, 0x52, 0x05, 0x76, 0x61,
	0x6c, 0x75, 0x65, 0x22, 0x3a, 0x0a, 0x0e, 0x54, 0x61, 0x72, 0x67, 0x65, 0x74, 0x53, 0x6f, 0x66,
	0x74, 0x77, 0x61, 0x72, 0x65, 0x12, 0x12, 0x0a, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x18, 0x01, 0x20,
	0x01, 0x28, 0x09, 0x52, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x12, 0x14, 0x0a, 0x05, 0x76, 0x61, 0x6c,
	0x75, 0x65, 0x18, 0x02, 0x20, 0x03, 0x28, 0x09, 0x52, 0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x22,
	0x74, 0x0a, 0x1a, 0x54, 0x61, 0x72, 0x67, 0x65, 0x74, 0x4f, 0x70, 0x65, 0x72, 0x61, 0x74, 0x69,
	0x6e, 0x67, 0x53, 0x79, 0x73, 0x74, 0x65, 0x6d, 0x43, 0x6c, 0x61, 0x73, 0x73, 0x12, 0x16, 0x0a,
	0x06, 0x76, 0x65, 0x6e, 0x64, 0x6f, 0x72, 0x18, 0x01, 0x20, 0x03, 0x28, 0x09, 0x52, 0x06, 0x76,
	0x65, 0x6e, 0x64, 0x6f, 0x72, 0x12, 0x1b, 0x0a, 0x09, 0x6f, 0x73, 0x5f, 0x66, 0x61, 0x6d, 0x69,
	0x6c, 0x79, 0x18, 0x02, 0x20, 0x03, 0x28, 0x09, 0x52, 0x08, 0x6f, 0x73, 0x46, 0x61, 0x6d, 0x69,
	0x6c, 0x79, 0x12, 0x21, 0x0a, 0x0c, 0x6d, 0x69, 0x6e, 0x5f, 0x61, 0x63, 0x63, 0x75, 0x72, 0x61,
	0x63, 0x79, 0x18, 0x03, 0x20, 0x01, 0x28, 0x0d, 0x52, 0x0b, 0x6d, 0x69, 0x6e, 0x41, 0x63, 0x63,
	0x75, 0x72, 0x61, 0x63, 0x79, 0x42, 0x7b, 0x0a, 0x18, 0x63, 0x6f, 0x6d, 0x2e, 0x67, 0x6f, 0x6f,
	0x67, 0x6c, 0x65, 0x2e, 0x74, 0x73, 0x75, 0x6e, 0x61, 0x6d, 0x69, 0x2e, 0x70, 0x72, 0x6f, 0x74,
	0x6f, 0x42, 0x1a, 0x50, 0x6c, 0x75, 0x67, 0x69, 0x6e, 0x52, 0x65, 0x70, 0x72, 0x65, 0x73, 0x65,
	0x6e, 0x74, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x50, 0x72, 0x6f, 0x74, 0x6f, 0x73, 0x50, 0x01, 0x5a,
	0x41, 0x67, 0x69, 0x74, 0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x67, 0x6f, 0x6f, 0x67,
	0x6c, 0x65, 0x2f, 0x74, 0x73, 0x75, 0x6e, 0x61, 0x6d, 0x69, 0x2d, 0x73, 0x65, 0x63, 0x75, 0x72,
	0x69, 0x74, 0x79, 0x2d, 0x73, 0x63, 0x61, 0x6e, 0x6e, 0x65, 0x72, 0x2f, 0x70, 0x72, 0x6f, 0x74,
	0x6f, 0x2f, 0x74, 0x73, 0x75, 0x6e, 0x61, 0x6d, 0x69, 0x5f, 0x67, 0x6f, 0x5f, 0x70, 0x72, 0x6f,
	0x74, 0x6f, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
})

var (
	file_plugin_representation_proto_rawDescOnce sync.Once
	file_plugin_representation_proto_rawDescData []byte
)

func file_plugin_representation_proto_rawDescGZIP() []byte {
	file_plugin_representation_proto_rawDescOnce.Do(func() {
		file_plugin_representation_proto_rawDescData = protoimpl.X.CompressGZIP(unsafe.Slice(unsafe.StringData(file_plugin_representation_proto_rawDesc), len(file_plugin_representation_proto_rawDesc)))
	})
	return file_plugin_representation_proto_rawDescData
}

var file_plugin_representation_proto_enumTypes = make([]protoimpl.EnumInfo, 1)
var file_plugin_representation_proto_msgTypes = make([]protoimpl.MessageInfo, 5)
var file_plugin_representation_proto_goTypes = []any{
	(PluginInfo_PluginType)(0),         // 0: tsunami.proto.PluginInfo.PluginType
	(*PluginDefinition)(nil),           // 1: tsunami.proto.PluginDefinition
	(*PluginInfo)(nil),                 // 2: tsunami.proto.PluginInfo
	(*TargetServiceName)(nil),          // 3: tsunami.proto.TargetServiceName
	(*TargetSoftware)(nil),             // 4: tsunami.proto.TargetSoftware
	(*TargetOperatingSystemClass)(nil), // 5: tsunami.proto.TargetOperatingSystemClass
}
var file_plugin_representation_proto_depIdxs = []int32{
	2, // 0: tsunami.proto.PluginDefinition.info:type_name -> tsunami.proto.PluginInfo
	3, // 1: tsunami.proto.PluginDefinition.target_service_name:type_name -> tsunami.proto.TargetServiceName
	4, // 2: tsunami.proto.PluginDefinition.target_software:type_name -> tsunami.proto.TargetSoftware
	5, // 3: tsunami.proto.PluginDefinition.target_operating_system_class:type_name -> tsunami.proto.TargetOperatingSystemClass
	0, // 4: tsunami.proto.PluginInfo.type:type_name -> tsunami.proto.PluginInfo.PluginType
	5, // [5:5] is the sub-list for method output_type
	5, // [5:5] is the sub-list for method input_type
	5, // [5:5] is the sub-list for extension type_name
	5, // [5:5] is the sub-list for extension extendee
	0, // [0:5] is the sub-list for field type_name
}

func init() { file_plugin_representation_proto_init() }
func file_plugin_representation_proto_init() {
	if File_plugin_representation_proto != nil {
		return
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: unsafe.Slice(unsafe.StringData(file_plugin_representation_proto_rawDesc), len(file_plugin_representation_proto_rawDesc)),
			NumEnums:      1,
			NumMessages:   5,
			NumExtensions: 0,
			NumServices:   0,
		},
		GoTypes:           file_plugin_representation_proto_goTypes,
		DependencyIndexes: file_plugin_representation_proto_depIdxs,
		EnumInfos:         file_plugin_representation_proto_enumTypes,
		MessageInfos:      file_plugin_representation_proto_msgTypes,
	}.Build()
	File_plugin_representation_proto = out.File
	file_plugin_representation_proto_goTypes = nil
	file_plugin_representation_proto_depIdxs = nil
}
